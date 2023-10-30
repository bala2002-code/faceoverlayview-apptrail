package vijay.vlr2k2.myapplicationartrial7;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {
    private ArFragment arFragment;
    private EditText nameEditText;
    private Button captureImageButton;
    private Button placeButton;
    private Button cameraButton;

    private final static int REQUEST_IMAGE_CAPTURE = 124;
    private FirebaseVisionImage image;
    private FirebaseVisionFaceDetector detector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        // Initialize AR components
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        nameEditText = findViewById(R.id.nameEditText);
        placeButton = findViewById(R.id.placeButton);
        captureImageButton = findViewById(R.id.captureImageButton);

        // Initialize ML Kit components
        cameraButton = findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(
                v -> {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    } else {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });

        // AR button to place text on the face
        placeButton.setOnClickListener(view -> {
            String name = nameEditText.getText().toString();
            if (!name.isEmpty()) {
                placeARTextOnFace(name);
            }
        });

        // AR button to capture image
        captureImageButton.setOnClickListener(view -> {
            captureImage();
        });
    }
    private void placeARTextOnFace(String text) {
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            Anchor anchor = hitResult.createAnchor();
            ViewRenderable.builder()
                    .setView(this, createTextViewOnFace(text))
                    .build()
                    .thenAccept(viewRenderable -> {
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setRenderable(viewRenderable);
                        arFragment.getArSceneView().getScene().addChild(anchorNode);
                    });
        });
    }

    private View createTextViewOnFace(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(30);
        textView.setTextColor(Color.WHITE);
        return textView;
    }

    private void captureImage() {
        ArSceneView arSceneView = arFragment.getArSceneView();
        Bitmap bitmap = Bitmap.createBitmap(arSceneView.getWidth(), arSceneView.getHeight(), Bitmap.Config.ARGB_8888);
        PixelCopy.request(arSceneView, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                detectFaceAndPlaceText(bitmap);
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }, new Handler());
    }

    private void detectFaceAndPlaceText(Bitmap bitmap) {
        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions
                .Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build();

        try {
            image = FirebaseVisionImage.fromBitmap(bitmap);
            detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
        detector.detectInImage(image)
                .addOnSuccessListener(firebaseVisionFaces -> {
                    if (!firebaseVisionFaces.isEmpty()) {
                        FirebaseVisionFace face = firebaseVisionFaces.get(0); // Assuming only one face is detected
                        String name = nameEditText.getText().toString();
                        if (!name.isEmpty()) {
                            placeARTextOnDetectedFace(name, face);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No face detected", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast
                        .makeText(
                                MainActivity.this,
                                "Oops, Something went wrong",
                                Toast.LENGTH_SHORT)
                        .show());
    }

    private void placeARTextOnDetectedFace(String text, FirebaseVisionFace face) {
        Rect re = face.getBoundingBox();

        float[] facePos = getBoundingBoxCenter(face);
        float poseX = facePos[0];
        float poseY = facePos[1];
        float poseZ = facePos[2];
        float poseScale = face.getBoundingBox().width() * 1.5f; // Adjust scale as needed
        Pose pose = Pose.makeTranslation(poseX, poseY, poseZ);
//        Pose pose = new Pose(poseX, poseY, poseZ, 0.0f, 0.0f, 0.0f, 0.0f);
        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
        ViewRenderable.builder()
                .setView(this, createTextViewOnFace(text))
                .build()
                .thenAccept(viewRenderable -> {
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setRenderable(viewRenderable);
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                });
    }
    float[] getBoundingBoxCenter(FirebaseVisionFace face) {
        Rect re = face.getBoundingBox();
        float centerX = re.centerX();
        float centerY = re.centerY();
        float centerZ = 0.0f; // Assume the face is on the same plane as the camera
        return new float[]{centerX, centerY, centerZ};
    }

}
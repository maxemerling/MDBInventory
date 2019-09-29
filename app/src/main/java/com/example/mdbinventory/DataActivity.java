package com.example.mdbinventory;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class DataActivity extends AppCompatActivity {

    private EditText cost, description, suppliers, dateText;
    private EditText[] textArr;
    private Button upload, chooseImage, show;
    private ImageView imageView;

    private final int PICK_IMAGE_REQUEST = 71;

    private Uri filePath;

    //Firebase
    FirebaseStorage storage;
    StorageReference storageReference;

    // recycler stuff
    RecyclerView recycler;
    RecyclerView.LayoutManager linearManager;
    RecyclerView.Adapter adapter;

    ArrayList<Transaction> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        cost = findViewById(R.id.cost);
        description = findViewById(R.id.description);
        suppliers = findViewById(R.id.supplier);
        dateText = findViewById(R.id.date);

        imageView = findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.transaction);

        upload = findViewById(R.id.upload);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkValidity()) {
                    uploadData();
                }
            }
        });

        chooseImage = findViewById(R.id.imageButton);

        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        show = findViewById(R.id.menuButton);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        data = new ArrayList<>();
        for (int i=0; i<10; i++) { // fill in tester data
            String date = String.format("01/0%s/1999", 9-i);
            data.add(new Transaction((float) 5.0, "description"+i, "suppliers"+i, "image_link"+i, date));
        }
        Collections.sort(data);

        // Setting up the RecyclerView
        recycler = findViewById(R.id.recycler);
        linearManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(linearManager); // default layout is linear
        adapter = new Adapter(this, data);
        recycler.setAdapter(adapter);
    }

    private static boolean isEmpty(EditText text) {
        return text.getText().toString().trim().isEmpty();
    }

    /**
     * Checks if date is formated like mm/dd/yyyy
     */
    private static boolean isDateFormatted(String date) {
        // check if the format is ##/##/####
        if (date.length() == 10) {
            for (int i = 0; i < date.length(); i++) {
                if (i == 2 || i == 5) {
                    if (date.charAt(i) != '/') {
                        return false;
                    }
                } else if (!Character.isDigit(date.charAt(i))) {
                    return false;
                }
            }
        } else {
            return false;
        }

        date = date.replace("/", "-");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
        try {
            simpleDateFormat.parse(date);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }


    private boolean checkValidity() {

        boolean returnVal = true;

        if (isEmpty(cost)) {
            cost.setError("Must enter a cost");
            return false;
        }

        if (isEmpty(suppliers)) {
            suppliers.setError("Must enter a supplier");
            return false;
        }

        if (isEmpty(description)) {
            description.setError("Must enter a description");
            return false;
        }

        if (isEmpty(dateText)) {
            dateText.setError("Must enter a date");
            return false;
        }

        if (!isDateFormatted(dateText.getText().toString())) {
            dateText.setError("Enter a date as mm/dd/yyyy");
            return false;
        }

        return true;
    }

    public Transaction findTransaction(String supplier, String date) {
        for (Transaction t : data) {
            if (t.suppliers.equals(supplier) && t.date.equals(date)) {
                return t;
            }
        }
        Log.d("ERROR:","TRANSACTION NOT IN DATA LIST");
        return null;
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Uploads the current image and returns a downloadable (read-only) link.
     */
    private void uploadData() {

        if (filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            final StorageReference ref = storageReference.child("images/"+ UUID.randomUUID() + ".png");

            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(DataActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(DataActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+ (int) progress+"%");
                        }
                    })
                    .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return ref.getDownloadUrl();
                        }
                    }).addOnCompleteListener(DataActivity.this, new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {

                                //this is the download url for the image -- save it with the Transaction object so that we can later display the image again
                                String downloadUrl = task.getResult().toString();

                                Transaction currTransaction =
                                        new Transaction(Float.parseFloat(cost.getText().toString()), description.getText().toString(),
                                                suppliers.getText().toString(), dateText.getText().toString(), downloadUrl);

                                DataManagement.writeNewTransaction(currTransaction, FirebaseDatabase.getInstance().getReference());
                            }
                        }
                    }
            );
        } else {
            Toast.makeText(DataActivity.this, "Null Filepath", Toast.LENGTH_SHORT).show();
        }
    }
}

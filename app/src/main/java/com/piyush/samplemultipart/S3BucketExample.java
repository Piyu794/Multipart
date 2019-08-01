package com.piyush.samplemultipart;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class S3BucketExample extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 100;
    AmazonS3 s3Client;
    String fileName;
    String bucket = "mytechaditya";
    File uploadToS3 = new File("/storage/sdcard0/Pictures/Screenshots/Screenshot.png");
    File downloadFromS3;
    TransferUtility transferUtility;
    List<String> listing;

    private String[] permissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_s3_bucket_example);

        permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

        // callback method to call credentialsProvider method.
        s3credentialsProvider();

        // callback method to call the setTransferUtility method
        setTransferUtility();
    }

    public void s3credentialsProvider() {

        // Initialize the AWS Credential
        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider =
                new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        "ca-central-1:2ab94451-83d0-4e76-b07a-10f5599afc6c", // Identity Pool ID
                        Regions.AP_SOUTH_1 // Region
                );
        createAmazonS3Client(cognitoCachingCredentialsProvider);
    }

    /**
     * Create a AmazonS3Client constructor and pass the credentialsProvider.
     *
     * @param credentialsProvider
     */
    public void createAmazonS3Client(CognitoCachingCredentialsProvider credentialsProvider) {
        // Create an S3 client
        s3Client = new AmazonS3Client(credentialsProvider);
        // Set the region of your S3 bucket
        s3Client.setRegion(Region.getRegion(Regions.AP_SOUTH_1));
    }

    public void setTransferUtility() {
        transferUtility = new TransferUtility(s3Client, getApplicationContext());
    }

    /**
     * This method is used to Download the file to S3 by using transferUtility class
     *
     * @param view
     **/
    public void downloadFileFromS3(View view) {
        Log.e("check_dwnld", bucket + ":" + fileName + ":" + downloadFromS3);
        TransferObserver transferObserver = transferUtility.download(
                bucket,     /* The bucket to download from */
                fileName,    /* The key for the object to download */
                downloadFromS3        /* The file to download the object to */
        );
        transferObserverListener(transferObserver);
    }

    public void fetchFileFromS3(View view) {
        // Get List of files from S3 Bucket
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                    listing = getObjectNamesForBucket(bucket, s3Client);

                    for (int i = 0; i < listing.size(); i++) {
                        Log.e("check_files", listing.get(i));
                        if (i == 1) {
                            fileName = listing.get(i);
                            downloadFromS3 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download" + fileName);
                        }
                        Toast.makeText(S3BucketExample.this, listing.get(i), Toast.LENGTH_LONG).show();
                    }
                    Looper.loop();
                    // Log.e("tag", "listing "+ listing);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("tag", "Exception found while listing " + e);
                }

            }
        });
        thread.start();
    }

    /**
     * @param bucket
     * @param s3Client
     * @return object with list of files
     * @desc This method is used to return list of files name from S3 Bucket
     */
    private List<String> getObjectNamesForBucket(String bucket, AmazonS3 s3Client) {
        ObjectListing objects = s3Client.listObjects(bucket);
        List<String> objectNames = new ArrayList<String>(objects.getObjectSummaries().size());
        Iterator<S3ObjectSummary> iterator = objects.getObjectSummaries().iterator();
        while (iterator.hasNext()) {
            objectNames.add(iterator.next().getKey());
        }
        while (objects.isTruncated()) {
            objects = s3Client.listNextBatchOfObjects(objects);
            iterator = objects.getObjectSummaries().iterator();
            while (iterator.hasNext()) {
                objectNames.add(iterator.next().getKey());
            }
        }
        return objectNames;
    }

    /**
     * This is listener method of the TransferObserver
     * Within this listener method, we get status of uploading and downloading file,
     * to display percentage of the part of file to be uploaded or downloaded to S3
     * It displays an error, when there is a problem in  uploading or downloading file to or from S3.
     *
     * @param transferObserver
     */

    public void transferObserverListener(TransferObserver transferObserver) {

        transferObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.e("check_obser", "On State Change");
                Toast.makeText(getApplicationContext(), "State Change"
                        + state, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                try {
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                    Log.e("check_obser", "On Progress Change");
                    Toast.makeText(getApplicationContext(), "Progress in %"
                            + percentage, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("check_obser", "On Error");
                Log.e("error", "error");
                ex.printStackTrace();
            }

        });
    }
}

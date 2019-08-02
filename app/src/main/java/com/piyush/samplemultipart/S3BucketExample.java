package com.piyush.samplemultipart;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    TransferUtility transferUtility;
    List<String> listing;

    private Adapter adapter;
    private String[] permissions;
    private SearchView searchView;
    private TextView tvNoFiles;
    private ProgressDialog loadingBar;
    private RecyclerView recyclerView;
    private DownloadListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_s3_bucket_example);

        recyclerView = findViewById(R.id.rvFiles);
        tvNoFiles = findViewById(R.id.tvNoFile);
        loadingBar = new ProgressDialog(this);
        loadingBar.setCancelable(false);
        loadingBar.setMessage("Fetching Files... Please Wait !!!");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        listing = new ArrayList<>();

        acceptPermissions();

        s3credentialsProvider();    // callback method to call credentialsProvider method.
        setTransferUtility();   // callback method to call the setTransferUtility method
    }

    private void acceptPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permissions[0]) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getApplicationContext(), permissions[1]) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(permissions, PERMISSION_REQ_CODE);
            else {
                if ((ContextCompat.checkSelfPermission(getApplicationContext(), permissions[0]) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(getApplicationContext(), permissions[1]) != PackageManager.PERMISSION_GRANTED))
                    requestPermissions(permissions, PERMISSION_REQ_CODE);
            }
        }
    }
    public void s3credentialsProvider() {
        // Initialize the AWS Credential
        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider =
                new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        AWSConfiguration.ID, // Identity Pool ID
                        Regions.US_EAST_2 // Region
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
        s3Client.setRegion(Region.getRegion(Regions.US_EAST_2));
    }
    public void setTransferUtility() {
        transferUtility = new TransferUtility(s3Client, getApplicationContext());
    }
    public void downloadFileFromS3(String fileName, File path) {
        this.fileName = fileName;
        Log.e("check_file_nm",fileName);
        TransferObserver transferObserver = transferUtility.download(
                AWSConfiguration.BUCKET,     /* The bucket to download from */
                fileName,    /* The key for the object to download */
                path /* The file to download the object to */
        );
        transferObserverListener(transferObserver);
    }
    public void fetchFileFromS3(View view) {
        // Get List of files from S3 Bucket
        loadingBar.show();
        tvNoFiles.setVisibility(View.GONE);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                    listing = getObjectNamesForBucket(AWSConfiguration.BUCKET, s3Client);
//                    for (int i = 0; i < listing.size(); i++) {
//                        Log.e("check_files", listing.get(i));
//                        if (i == 1) {
//                            fileName = listing.get(i);
//                            downloadFromS3 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download" + fileName);
//                        }
//                        Toast.makeText(S3BucketExample.this, listing.get(i), Toast.LENGTH_LONG).show();
//                    }
                    final List<String> tempList = new ArrayList<>();
                    for (String file : listing){
                        String[] arr = file.split("/");
                        if (arr.length > 1)
                            tempList.add(arr[arr.length-1]);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("check_list",listing.toString());
                            adapter = new Adapter(S3BucketExample.this,tempList);
                            recyclerView.setAdapter(adapter);
                            loadingBar.dismiss();
                            if (listing.isEmpty())
                                tvNoFiles.setVisibility(View.VISIBLE);
                        }
                    });
                    Looper.loop();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingBar.dismiss();
                            if (listing.isEmpty())
                                tvNoFiles.setVisibility(View.VISIBLE);
                        }
                    });
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
        for (S3ObjectSummary s3ObjectSummary : objects.getObjectSummaries()) {
            objectNames.add(s3ObjectSummary.getKey());
        }
        while (objects.isTruncated()) {
            objects = s3Client.listNextBatchOfObjects(objects);
            for (S3ObjectSummary s3ObjectSummary : objects.getObjectSummaries()) {
                objectNames.add(s3ObjectSummary.getKey());
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
                Log.e("check_obser", "On State "+state);
                if (state.name().equals("COMPLETED"))
                    listener.onFileDownloaded(fileName);
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                try {
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                    Log.e("check_obser", "On Progress Change");
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

    private boolean checkFolders(String objectName,String root){
        boolean isSubFile = false;
        String[] arr = objectName.split("/");
        if (arr.length > 2) {
            isSubFile = !arr[0].equalsIgnoreCase(root.substring(0, root.length() - 1));
        }
        Log.e("check_file",objectName+":"+isSubFile);
        return isSubFile;
    }
    public void setDownloadListener(DownloadListener listener){
        this.listener = listener;
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                adapter.getFilter().filter(query);
                return false;
            }
        });
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    interface DownloadListener {
        void onFileDownloaded(String file);
    }
}

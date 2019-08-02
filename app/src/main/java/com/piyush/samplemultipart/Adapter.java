package com.piyush.samplemultipart;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

public class Adapter extends RecyclerView.Adapter<Adapter.Holder> implements Filterable, S3BucketExample.DownloadListener {

    private String path;
    private List<String> files;
    private List<String> filteredFiles;
    private S3BucketExample context;
    private LayoutInflater inflater;

    Adapter(S3BucketExample context, List<String> list){
        this.files = list;
        this.context = context;
        this.filteredFiles = list;
        inflater = LayoutInflater.from(context);
        context.setDownloadListener(this);
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/";
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new Holder(inflater.inflate(R.layout.list_item,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int i) {
        final String file = filteredFiles.get(i);
        Drawable drawable;
        if (file.contains("/"))
            drawable = ContextCompat.getDrawable(context, R.drawable.icon_folder);
        else
            drawable = ContextCompat.getDrawable(context,R.drawable.icon_file);
        if (drawable != null)
            drawable.setBounds(0,0,24,24);
        holder.tvFile.setText(file.replace("/",""));
        holder.tvFile.setCompoundDrawablesWithIntrinsicBounds(drawable,null,null,null);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File location = new File(path+file);
                Log.e("check_loc",location.getAbsolutePath()+":"+location.exists());
                if (file.contains(".pdf"))
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AWSConfiguration.FILE_URI+file)));
                else if (location.exists())
                    openFile(context,location);
                else
                    context.downloadFileFromS3(AWSConfiguration.FILE_URI+file, new File(path+file));
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredFiles.size();
    }
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString();
                if (charString.isEmpty()) {
                    filteredFiles = files;
                } else {
                    List<String> filteredList = new ArrayList<>();
                    for (String file : files) {
                        if (file.toLowerCase().contains(charString.toLowerCase()) || file.contains(constraint))
                            filteredList.add(file);
                    }
                    filteredFiles = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredFiles;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredFiles = (ArrayList<String>) results.values;
                notifyDataSetChanged();
            }
        };
    }
    @Override
    public void onFileDownloaded(String file) {
        openFile(context,new File(path+file));
    }
    private void openFile(Context context, File url) {
        Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            uri = Uri.fromFile(url);
        else
            uri = FileProvider.getUriForFile(context,context.getPackageName(),url);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            Log.e("check_whete","url :"+url);
            if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
                // Word document
                intent.setDataAndType(uri, "application/msword");
            } else if (url.toString().contains(".pdf")) {
                // PDF file
                intent.setDataAndType(uri, "application/pdf");
            } else if (url.toString().contains(".xls") || url.toString().contains(".xlsx")) {
                // Excel file
                intent.setDataAndType(uri, "application/vnd.ms-excel");
            } else if (url.toString().contains(".zip") || url.toString().contains(".rar")) {
                // WAV audio file
                intent.setDataAndType(uri, "application/x-wav");
            } else if (url.toString().contains(".jpg") || url.toString().contains(".jpeg") || url.toString().contains(".png")) {
                // JPG file
                intent.setDataAndType(uri, "image/jpeg");
            } else if (url.toString().contains(".txt")) {
                // Text file
                intent.setDataAndType(uri, "text/plain");
            } else {
                //if you want you can also define the intent type for any other file

                //additionally use else clause below, to manage other unknown extensions
                //in this case, Android will show all applications installed on the device
                //so you can choose which application to use
                intent.setDataAndType(uri, "*/*");
            }
            intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e){
            Toast.makeText(context,"No App Found To Open This File !",Toast.LENGTH_SHORT).show();
        }
    }

    class Holder extends RecyclerView.ViewHolder {
        TextView tvFile;
        Holder(@NonNull View itemView) {
            super(itemView);
            tvFile = itemView.findViewById(R.id.tvFile);
        }
    }
}

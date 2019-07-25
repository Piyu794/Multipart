package com.piyush.samplemultipart;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface Apis {

    @Multipart
    @POST("/test/multipartRequest")
    Call<String> sendImage(@Part MultipartBody.Part file);

}

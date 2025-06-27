package com.example.dineroapp.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import okhttp3.*;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class ServicioAlmacenamiento {

    private static final String CLOUD_NAME = "du24xkcqt";
    private static final String UPLOAD_PRESET = "Lab7_20213801";
    @SuppressLint("AuthLeak")
    private static final String CLOUDINARY_URL = "cloudinary://473257155147649:EGUzD678aRldMa1rVJzPisUqEYg@du24xkcqt";
    private final OkHttpClient client = new OkHttpClient();
    private Context context;

    public ServicioAlmacenamiento(Context context) {
        this.context = context;
    }

    // Guardar archivo en la nube (Cloudinary)
    public void guardarArchivo(String nombreArchivo, Uri archivoUri, UploadCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(archivoUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", nombreArchivo,
                            RequestBody.create(imageBytes, MediaType.parse("image/*")))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        callback.onFailure(new Exception("Error al subir a Cloudinary: " + response.code()));
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String imageUrl = json.getString("secure_url");
                        callback.onSuccess(imageUrl);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure(e);
        }
    }


    // Obtener URL de un archivo específico (ya no se usa en Cloudinary directo)
    public void obtenerArchivo(String nombreArchivo, DownloadUrlCallback callback) {
        // Cloudinary no requiere este paso: la URL se obtiene al subir
        callback.onFailure(new UnsupportedOperationException("No se requiere obtener archivo por nombre en Cloudinary"));
    }

    // Interfaces de retorno de éxito o fallo
    public interface UploadCallback {
        void onSuccess(String url);
        void onFailure(Exception e);
    }

    public interface DownloadUrlCallback {
        void onSuccess(Uri uri);
        void onFailure(Exception e);
    }
}

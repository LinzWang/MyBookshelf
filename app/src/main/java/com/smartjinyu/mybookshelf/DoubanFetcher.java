package com.smartjinyu.mybookshelf;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.util.Log;

import androidx.annotation.NonNull;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by smartjinyu on 2017/1/20.
 * This class is used to get information from website like DouBan
 */

public class DoubanFetcher extends BookFetcher {
    private static final String TAG = "DoubanFetcher";


    @Override
    public void getBookInfo(final Context context, final String isbn, final int mode) {
        mContext = context;

        Retrofit mRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.jike.xyz/situ/book/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        DB_API api = mRetrofit.create(DB_API.class);
        //delete douban api ,call jike directly

        Call<DouBanJson> call = api.getDBResult(isbn);

        call.enqueue(new Callback<DouBanJson>() {
            @Override
            public void onResponse(@NonNull Call<DouBanJson> call, @NonNull Response<DouBanJson> response) {
                assert response.body() != null;
                Log.i(TAG,"ret is:"+response.body().getRet()+",isbn is"+isbn+",msg is:"+response.body().getMsg());
                if (response.code() == 200 &&  response.body().getRet() == 0) {
                    Log.i(TAG, "GET Douban information successfully, id = " + response.body().getData().getId()
                            + ", title = " + response.body().getData().getTitle());
                    mBook = new Book();
                    mBook.setTitle(response.body().getData().getTitle());
                    //mBook.setId(Long.parseLong(response.body().getId(),10));
                    mBook.setIsbn(isbn);
                    if (response.body().getData().getAuthor()!=null && response.body().getData().getAuthor().size() != 0) {
                        mBook.setAuthors(response.body().getData().getAuthor());
                    } else {
                        mBook.setAuthors(new ArrayList<>());
                    }
                    if (response.body().getData().getTranslator()!=null && response.body().getData().getTranslator().size() != 0) {
                        mBook.setTranslators(response.body().getData().getTranslator());
                    } else {
                        mBook.setTranslators(new ArrayList<>());
                    }

                    mBook.getWebIds().put("douban", response.body().getData().getId());
                    mBook.setPublisher(response.body().getData().getPublisher());

                    mBook.setTotalPage(response.body().getData().getPages());

                    String rawDate = response.body().getData().getPubdate();
                    Log.i(TAG, "Date raw = " + rawDate);
                    String year, month;
                    if (rawDate.contains("-")) {
                        // 2016-11
                        String[] date = rawDate.split("-");
                        year = date[0];
                        // rawDate sometimes is "2016-11", sometimes is "2000-10-1", sometimes is "2010-1"
                        month = date[1];
                    } else if (rawDate.contains(".")) {
                        String[] date = rawDate.split("\\.");
                        year = date[0];
                        // rawDate sometimes is "2016-11", sometimes is "2000-10-1", sometimes is "2010-1"
                        month = date[1];
                    } else {
                        year = "9999";
                        month = "1";
                    }
                    Log.i(TAG, "Get PubDate Year = " + year + ", month = " + month);
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Integer.parseInt(year), Integer.parseInt(month) - 1, 1);
                    mBook.setPubTime(calendar);
                    //String imgUrl = response.body().getData().getimage();
                    //String pattern = "img\\d\\.";
                    final String imageURL = response.body().getData().getimage();
                    Log.i(TAG,"image url :"+imageURL);
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
                    boolean addWebsite = pref.getBoolean("settings_pref_acwebsite", true);
                    if (addWebsite) {
                        mBook.setWebsite("https://book.douban.com/subject/" + response.body().getData().getId());
                    }
                    if (mode == 0) {
                        ((SingleAddActivity) mContext).fetchSucceed(mBook, imageURL);
                    } else if (mode == 1) {
                        ((BatchAddActivity) mContext).fetchSucceed(mBook, imageURL);
                    }
                } else {
                    Log.w(TAG, "Unexpected response code " + response.code() + ", isbn = " + isbn);
                    if (mode == 0) {
                        ((SingleAddActivity) mContext).fetchFailed(
                                BookFetcher.fetcherID_DB, 0, isbn
                        );
                    } else if (mode == 1) {
                        ((BatchAddActivity) mContext).fetchFailed(
                                BookFetcher.fetcherID_DB, 0, isbn);
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<DouBanJson> call, @NonNull Throwable t) {
                Log.w(TAG, "GET Douban information failed, " + t.toString());
                if (mode == 0) {
                    ((SingleAddActivity) mContext).fetchFailed(
                            BookFetcher.fetcherID_DB, 1, isbn
                    );
                } else if (mode == 1) {
                    ((BatchAddActivity) mContext).fetchFailed(
                            BookFetcher.fetcherID_DB, 1, isbn);
                }
            }
        });


    }

    private interface DB_API {
        @GET("isbn/{isbn}")
        Call<DouBanJson> getDBResult(@Path("isbn") String isbn);
    }

}







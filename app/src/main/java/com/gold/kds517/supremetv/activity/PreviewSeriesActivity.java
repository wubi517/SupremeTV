package com.gold.kds517.supremetv.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gold.kds517.supremetv.R;
import com.gold.kds517.supremetv.adapter.SeriesListAdapter;
import com.gold.kds517.supremetv.apps.Constants;
import com.gold.kds517.supremetv.apps.MyApp;
import com.gold.kds517.supremetv.dialog.PackageDlg;
import com.gold.kds517.supremetv.dialog.SearchSeriesDlg;
import com.gold.kds517.supremetv.models.CategoryModel;
import com.gold.kds517.supremetv.models.SeriesModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class PreviewSeriesActivity extends AppCompatActivity implements View.OnClickListener,AdapterView.OnItemClickListener{
    List<CategoryModel> categoryModels;
    List<SeriesModel>seriesModels;
    SeriesListAdapter adapter;
    RelativeLayout main_lay;
    ImageView btn_back,image_movie,btn_about_movie;
    TextView txt_category,txt_time,txt_progress,txt_rating,txt_plot,txt_director,txt_genre;
    ListView vod_list;
    Button btn_play,btn_trailer,btn_fav,btn_search;
    String sort_by = "";
    String category_id,vod_title,vod_image,mStream_id,vod_star,vod_cast,vod_genre,vod_plot,vod_cat_id;
    int category_pos,sub_pos=0,sort = 0;
    List<String> pkg_datas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_series);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        pkg_datas = new ArrayList<>();
        pkg_datas.addAll(Arrays.asList(getResources().getStringArray(R.array.vod_package)));

        txt_category = findViewById(R.id.txt_category);
        category_pos = (int) MyApp.instance.getPreference().get(Constants.getSeriesPos());

        categoryModels = MyApp.series_categories_filter;
        category_id = categoryModels.get(category_pos).getId();
        txt_category.setText(categoryModels.get(category_pos).getName());

        main_lay = findViewById(R.id.main_lay);

        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        btn_about_movie = findViewById(R.id.btn_about_movie);
        btn_about_movie.setOnClickListener(this);
        image_movie = findViewById(R.id.image_movie);

        txt_time = findViewById(R.id.txt_time);
        txt_progress = findViewById(R.id.txt_progress);
        txt_rating = findViewById(R.id.txt_rating);
        txt_plot = findViewById(R.id.txt_plot);
        txt_director = findViewById(R.id.txt_director);
        txt_genre = findViewById(R.id.txt_genre);

        findViewById(R.id.ly_back).setOnClickListener(this);

        vod_list = findViewById(R.id.vod_list);
        vod_list.setOnItemClickListener(this);
        vod_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sub_pos =i;
                printSeriesInfo();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btn_play = findViewById(R.id.btn_play);
        btn_trailer = findViewById(R.id.btn_trailer);
        btn_fav = findViewById(R.id.btn_fav);
        btn_fav.setOnClickListener(this);
        btn_search = findViewById(R.id.btn_search);
        btn_search.setOnClickListener(this);
        btn_play.setOnClickListener(this);
        btn_trailer.setOnClickListener(this);

        Thread myThread = null;
        Runnable runnable = new CountDownRunner();
        myThread = new Thread(runnable);
        myThread.start();
        sort = (int) MyApp.instance.getPreference().get(Constants.getSORT());
        seriesModels = MyApp.fullSeriesModels_filter.get(category_pos).getChannels();
        MyApp.seriesModels = seriesModels;
        if(category_pos==1){
            GetSortModel(seriesModels);
            txt_category.setText(categoryModels.get(category_pos).getName()+" /"+sort_by);
        }else {
            txt_category.setText(categoryModels.get(category_pos).getName());
        }
        adapter = new SeriesListAdapter(this,seriesModels);
        vod_list.setAdapter(adapter);
        vod_list.requestFocus();
        vod_list.setSelection(sub_pos);
        vod_list.performClick();
        printSeriesInfo();
        FullScreencall();
    }
    private  List<SeriesModel> GetSortModel(List<SeriesModel>movieModels){
        switch (sort){
            case 0:
                sort_by = "By Number";
                Collections.sort(movieModels,
                        (o1, o2) -> {
                            try {
                                if (Long.parseLong(o1.getNum()) <
                                        Long.parseLong(o2.getNum()))
                                {
                                    return -1;
                                }
                            }catch (Exception e){

                            }
                            return 1;
                        });
                break;
            case 1:
                sort_by = "By Added";
                Collections.sort(movieModels,
                        (o1, o2) -> {
                            try {
                                if (Long.parseLong(o1.getAdded()) ==
                                        Long.parseLong(o2.getAdded()))
                                {
                                    return 0;
                                }
                                else if (Long.parseLong(o1.getAdded()) >
                                        Long.parseLong(o2.getAdded()))
                                {
                                    return -1;
                                }
                            }catch (Exception e){

                            }
                            return 1;
                        });
                break;
            case 2:
                sort_by = "By Name";
                Collections.sort(movieModels, (o1, o2) -> o1.getName().compareTo(o2.getName()));
                break;
        }
        return movieModels;
    }

    private void printSeriesInfo(){
        if(seriesModels!=null && seriesModels.size()>0){
            mStream_id = seriesModels.get(sub_pos).getSeries_id();
            if(seriesModels.get(sub_pos).getYoutube()!=null && !seriesModels.get(sub_pos).getYoutube().isEmpty()){
                btn_trailer.setVisibility(View.VISIBLE);
            }else {
                btn_trailer.setVisibility(View.GONE);
            }
            txt_rating.setText(seriesModels.get(sub_pos).getRating());
            txt_plot.setText(seriesModels.get(sub_pos).getPlot());
            txt_director.setText(seriesModels.get(sub_pos).getDirector());
            txt_genre.setText(seriesModels.get(sub_pos).getGenre());
            try {
                Picasso.with(PreviewSeriesActivity.this).load(seriesModels.get(sub_pos).getStream_icon())
                        .placeholder(R.drawable.icon_default)
                        .error(R.drawable.icon_default)
                        .into(image_movie);
            }catch (Exception e){
                Picasso.with(PreviewSeriesActivity.this).load(R.drawable.icon_default).into(image_movie);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_play:
                PlaySeries();
                break;
            case R.id.ly_back:
                finish();
                break;
            case R.id.btn_trailer:
                PlayTrailer();
                break;
            case R.id.btn_search:
                searchSeries();
                break;
            case R.id.btn_fav:
                setFav();
                break;
        }
    }

    private void searchSeries(){
        SearchSeriesDlg searchSeriesDlg = new SearchSeriesDlg(this, (dialog, sel_Channel) -> {
            dialog.dismiss();
            FullScreencall();
            for(int i = 0;i<seriesModels.size();i++){
                if(seriesModels.get(i).getName().equalsIgnoreCase(sel_Channel.getName())){
                    sub_pos = i;
                    break;
                }
            }
            scrollToLast(vod_list, sub_pos);
            printSeriesInfo();
            MyApp.touch = true;
            adapter.selectItem(sub_pos);
        });
        searchSeriesDlg.show();
    }

    private void PlaySeries(){
        if(seriesModels!=null && seriesModels.size()>0){
            SeriesModel showmodel = seriesModels.get(sub_pos);
            checkAddedRecent(showmodel);
            Constants.getRecentFullSeriesModel(MyApp.fullSeriesModels_filter).getChannels().add(0,showmodel);
            //get recent channel names list

            List<String> recent_channel_models = new ArrayList<>();
            for (SeriesModel channel:Constants.getRecentFullSeriesModel(MyApp.fullSeriesModels_filter).getChannels()){
                recent_channel_models.add(channel.getName());
            }
            MyApp.instance.getPreference().put(Constants.getRecentSeries(), recent_channel_models);

            vod_title = seriesModels.get(sub_pos).getName();
            vod_image = seriesModels.get(sub_pos).getStream_icon();
            vod_star = seriesModels.get(sub_pos).getRating();
            vod_cast = seriesModels.get(sub_pos).getCast();
            vod_genre = seriesModels.get(sub_pos).getGenre();
            vod_cat_id = category_id;
            Intent intent = new Intent(this,SeriesCatActivity.class);
            intent.putExtra("title",vod_title);
            intent.putExtra("star",vod_star);
            intent.putExtra("cast",vod_cast);
            intent.putExtra("genre",vod_genre);
            intent.putExtra("plot",vod_plot);
            intent.putExtra("cat_id",vod_cat_id);
            intent.putExtra("img_url",vod_image);
            intent.putExtra("series_id",seriesModels.get(sub_pos).getSeries_id());
            startActivity(intent);
        }
    }

    private void PlayTrailer(){
        if(seriesModels!=null && seriesModels.size()>0){
            String content_Uri = seriesModels.get(sub_pos).getYoutube();
            if(content_Uri.isEmpty()){
                Toast.makeText(getApplicationContext(),"This movie do not have trailer",Toast.LENGTH_LONG).show();
                return;
            }else {
                String newstr = content_Uri;
                if(content_Uri.contains("=")){
                    int endIndex = content_Uri.lastIndexOf("=");
                    if (endIndex != -1)
                    {
                        newstr = content_Uri.substring(endIndex+1); // not forgot to put check if(endIndex != -1)
                    }
                }else{
                    int endIndex = content_Uri.lastIndexOf("/");
                    if (endIndex != -1)
                    {
                        newstr = content_Uri.substring(endIndex+1); // not forgot to put check if(endIndex != -1)
                    }
                }
                Intent intent1 = new Intent(this, TrailerActivity.class);
                intent1.putExtra("content_Uri",newstr);
                startActivity(intent1);
            }
        }
    }

    private void setFav(){
        if (MyApp.fullSeriesModels_filter.get(category_pos).getChannels().get(sub_pos).isIs_favorite()) {
            pkg_datas.set(0, "Add to Fav");
            MyApp.fullSeriesModels_filter.get(category_pos).getChannels().get(sub_pos).setIs_favorite(false);
            boolean is_exist = false;
            int pp = 0;
            for (int i = 0; i < Constants.getFavFullSeries(MyApp.fullSeriesModels_filter).getChannels().size(); i++) {
                if (Constants.getFavFullSeries(MyApp.fullSeriesModels_filter).getChannels().get(i).getName().equals(MyApp.fullSeriesModels_filter.get(category_pos).getChannels().get(sub_pos).getName())) {
                    is_exist = true;
                    pp = i;
                }
            }
            if (is_exist)
                Constants.getFavFullSeries(MyApp.fullSeriesModels_filter).getChannels().remove(pp);
            MyApp.instance.getPreference().put(Constants.getSeriesFav(), Constants.getListStrFromListSeries(Constants.getFavFullSeries(MyApp.fullSeriesModels_filter).getChannels()));
        } else {
            MyApp.fullSeriesModels_filter.get(category_pos).getChannels().get(sub_pos).setIs_favorite(true);
            Constants.getFavFullSeries(MyApp.fullSeriesModels_filter).getChannels().add(MyApp.fullSeriesModels_filter.get(category_pos).getChannels().get(sub_pos));
            MyApp.instance.getPreference().put(Constants.getSeriesFav(), Constants.getListStrFromListSeries(Constants.getFavFullSeries(MyApp.fullSeriesModels_filter).getChannels()));
            pkg_datas.set(0, "Remove from Fav");
        }
        adapter.selectItem(sub_pos);
    }
    private void checkAddedRecent(SeriesModel showMovieModel) {
        Iterator<SeriesModel> iter = Constants.getRecentFullSeriesModel(MyApp.fullSeriesModels_filter).getChannels().iterator();
        while(iter.hasNext()){
            SeriesModel movieModel = iter.next();
            if (movieModel.getName().equals(showMovieModel.getName()))
                iter.remove();
        }
    }
    private void scrollToLast(final ListView listView, final int position) {
        listView.post(() -> listView.setSelection(position));
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(adapterView==vod_list){
            if(seriesModels.get(sub_pos).getName().equalsIgnoreCase(seriesModels.get(i).getName())){
                PlaySeries();
            }else {
                sub_pos = i;
                printSeriesInfo();
                MyApp.touch = true;
                adapter.selectItem(sub_pos);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View view = getCurrentFocus();
        if (event.getAction() == KeyEvent.ACTION_DOWN){
            switch (event.getKeyCode()){
                case KeyEvent.KEYCODE_MENU:
                    if (MyApp.fullSeriesModels_filter.get(category_pos).getChannels().get(sub_pos).isIs_favorite()) {
                        pkg_datas.set(0,"Remove from Fav");
                    }else {
                        pkg_datas.set(0,"Add to Fav");
                    }
                    PackageDlg packageDlg = new PackageDlg(this, pkg_datas, (dialog, position) -> {
                        dialog.dismiss();
                        switch (position){
                            case 0:
                                setFav();
                                break;
                            case 1:
                                PlayTrailer();
                                break;
                            case 2:
                                searchSeries();
                                break;
                        }
                    });
                    packageDlg.show();
                    break;
                case KeyEvent.KEYCODE_BACK:
                    finish();
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if(view==btn_fav || view == btn_search || view==btn_trailer||view==btn_play){
//                         vod_list.requestFocus();
//                         vod_list.setSelection(sub_pos);
//                         vod_list.performClick();
                    }else {
                        finish();
                    }

                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if(view==vod_list){
                        if(sub_pos==0){
                            sub_pos = seriesModels.size()-1;
                            printSeriesInfo();
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if(view==vod_list){
                        if(sub_pos==seriesModels.size()-1){
                            sub_pos=0;
                            printSeriesInfo();
                        }
                    }
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    class CountDownRunner implements Runnable {
        // @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doWork();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }
        }
    }

    public void doWork() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    txt_time.setText(Constants.clockFormat.format(new Date()));
                } catch (Exception e) {
                }
            }
        });
    }
    public void FullScreencall() {
        if(Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else  {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}

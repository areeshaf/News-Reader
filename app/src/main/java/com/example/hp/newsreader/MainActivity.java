package com.example.hp.newsreader;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles=new ArrayList<>();
    ArrayList<String> content=new ArrayList<>();
    SQLiteDatabase sqLiteDatabase;
    ListView listView;
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sqLiteDatabase=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleid INTEGER,title VARCHAR,content VARCHAR) ");


        DownloadTask task=new DownloadTask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

        listView=findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        updateListView();

    }

    public void updateListView(){
        Cursor cursor=sqLiteDatabase.rawQuery("SELECT * FROM articles",null);
        int titleIndex=cursor.getColumnIndex("title");
        int contentIndex=cursor.getColumnIndex("content");

        if(cursor.moveToFirst()) {
            titles.clear();
            content.clear();

            do {
                titles.add(cursor.getString(titleIndex));
                content.add(cursor.getString(contentIndex));
            } while (cursor.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }
    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {

            String result="";
            URL url;
            HttpURLConnection urlConnection=null;
            try{
                url=new URL(strings[0]);
                urlConnection=(HttpURLConnection)url.openConnection();
                InputStream inputStream=urlConnection.getInputStream();
                InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
                int data=inputStreamReader.read();

                while (data!=-1){
                    char current=(char)data;
                    result+=current;
                    data=inputStreamReader.read();
                }

                JSONArray jsonArray=new JSONArray(result);
                int numberOfNews=2;
                if(jsonArray.length()<2){
                    numberOfNews=jsonArray.length();
                }
                sqLiteDatabase.execSQL("DELETE FROM articles");
                for(int i=0;i<numberOfNews;i++){
                    String articleId=jsonArray.getString(i);
                   try{
                        url=new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                        urlConnection=(HttpURLConnection)url.openConnection();
                        inputStream=urlConnection.getInputStream();
                        inputStreamReader=new InputStreamReader(inputStream);
                        data=inputStreamReader.read();
                        String article="";
                        while(data!=-1){
                            char current = (char)data;
                            article+=current;
                            data=inputStreamReader.read();
                        }

                        JSONObject jsonObject=new JSONObject(article);
                        if(!jsonObject.isNull("title")&& !jsonObject.isNull("url")) {
                            String articleTitle = jsonObject.getString("title");
                            String articleURL = jsonObject.getString("url");

                            url = new URL(articleURL);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            inputStream = urlConnection.getInputStream();
                            inputStreamReader = new InputStreamReader(inputStream);
                            data = inputStreamReader.read();
                            String htmlURLcontent = "";
                            while (data != -1) {
                                char current = (char) data;
                                htmlURLcontent += current;
                                data = inputStreamReader.read();
                            }
                            Log.i("HTML_URL", htmlURLcontent);

                            String sql="INSERT INTO articles(articleId,title,content) VALUES (?,?,?)";
                            SQLiteStatement sqLiteStatement=sqLiteDatabase.compileStatement(sql);
                            sqLiteStatement.bindString(1,articleId);
                            sqLiteStatement.bindString(2,articleTitle);
                            sqLiteStatement.bindString(3,articleURL);
                            sqLiteStatement.execute();


                        }
                        // Log.i("Title and URL",articleTitle+"#######"+articleURL);
                       // Log.i("ARTICLE",article);

                    }catch (Exception e){
                        e.printStackTrace();
                   }
                }
               Log.i("Data",result);
                return result;

            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

}

package com.example.smart_post_office;

import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.smart_post_office.Network.NetWorkUtil;
import com.example.smart_post_office.util.Config;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    //view Objects
    //private ImageButton buttonScan;

    private Button btnLogIn;
    private ImageButton btnScan;
    private TextView txtUser;
    private TextView txtPhone;
    private TextView txtEmail;
    private TextView txtAddress;
    private TextView txtPoint;
    private NetWorkUtil netWorkUtil;
    private String userOid;
    private int userPoint;

    //qr code scanner object
    private IntentIntegrator qrScan;
    private SharedPreferences getUser;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        netWorkUtil = new NetWorkUtil(getApplicationContext());
        //세션 가져오기
        getUser = getApplicationContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        editor = getUser.edit();
        Log.e("session",getUser.getAll().toString());
        //UI 구성
        btnScan = (ImageButton) findViewById(R.id.btnScan);

        btnLogIn =(Button)findViewById(R.id.btnLogIn);
        txtUser =(TextView)findViewById(R.id.txtUser);
        txtPhone = (TextView)findViewById(R.id.txtPhone);
        txtEmail=(TextView)findViewById(R.id.txtEmail);
        txtAddress=(TextView)findViewById(R.id.txtAddress);
        txtPoint=(TextView)findViewById(R.id.txtPoint);

        //세션 값이 있을때
        if(getUser.contains("userId")) {
            btnLogIn.setText("로그아웃");
            btnLogIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.remove("userPoint");
                    editor.remove("userPhone");
                    editor.remove("userId");
                    editor.remove("userAddress");
                    editor.remove("userOid");
                    editor.remove("userName");
                    editor.commit();

                    Intent intent = new Intent(getApplicationContext(),
                            LoginActivity.class);
                    startActivity(intent);
                }
            });

            String userPoint = Integer.toString(getUser.getInt("userPoint",0));
            txtUser.setText(getUser.getString("userName","")+"님 환영합니다.");
            txtPhone.setText(getUser.getString("userPhone",""));
            txtEmail.setText(getUser.getString("userId",""));
            txtAddress.setText(getUser.getString("userAddress",""));
            txtPoint.setText(userPoint);
        }else{
            //로그인 눌렀을때
            btnLogIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(),
                            LoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        //intializing scan object
        qrScan = new IntentIntegrator(this);

        //qr 코드 눌렀을떄
        btnScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //scan option
                qrScan.setPrompt("Scanning...");
                //qrScan.setOrientationLocked(false);
                qrScan.initiateScan();
            }
        });
    }


    private String groupOid, deliveryOid, reciverOid;

   // qr 코드 동작코드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //qrcode 가 없으면
            if (result.getContents() == null) {
                Toast.makeText(MainActivity.this, "취소!", Toast.LENGTH_SHORT).show();
            } else {
                //qrcode 결과가 있으면
                Toast.makeText(MainActivity.this, "스캔완료!", Toast.LENGTH_SHORT).show();
                try {
                    //data를 json으로 변환
                    JSONObject obj = new JSONObject(result.getContents());
                    groupOid = obj.getString("groupoid");
                    deliveryOid = obj.getString("useroid");
                    reciverOid = obj.getString("phone");
                    postSendToChain(groupOid,deliveryOid);
                    Toast.makeText(MainActivity.this, "수취인이 다릅니다.", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                    //Toast.makeText(MainActivity.this, result.getContents(), Toast.LENGTH_LONG).show();
                    Log.e("catch",result.getContents());
                    Toast.makeText(MainActivity.this, result.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void postSendToChain(String groupOid, String deliveryOid) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("group",groupOid);
            jsonObject.put("delivery",deliveryOid);
            userOid = getUser.getString("userOid","");
            jsonObject.put("reciver",userOid);
            netWorkUtil.requestServer(Request.Method.POST,Config.POST_CHAIN,jsonObject,networkSuccessListener(),networkErrorListener());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Response.Listener<JSONObject> networkSuccessListener() {
        return new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject response) {
                netWorkUtil.requestServer(Config.GET_CHAIN+userOid,networkGetSuccessListener(),networkGetErrorListener());
                Log.e("success","success");
            }
        };

    }
    private Response.ErrorListener networkErrorListener() {
        return new Response.ErrorListener() {

            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private Response.Listener<JSONArray> networkGetSuccessListener() {
        return new Response.Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray response) {
                try {
                    Log.e("responsearray",response.toString());
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for(int i=0; i < jsonArray.length();i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        userPoint = jsonObject.getInt("point");
                        Log.e("point",Integer.toString(userPoint));
                        editor.putInt("userPoint",userPoint).apply();
                    }
                    setResult(RESULT_OK);//////////////////////////////////////////////////////

                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener networkGetErrorListener() {
        return new Response.ErrorListener() {

            public void onErrorResponse(VolleyError error) {
                Log.e("ffffff","에러");
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }
    @Override
    public void onResume() {
        super.onResume();
        btnLogIn.setText("로그아웃");
        btnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.remove("userPoint");
                editor.remove("userPhone");
                editor.remove("userId");
                editor.remove("userAddress");
                editor.remove("userOid");
                editor.remove("userName");
                editor.apply();
                Log.e("delete","d");
                Intent intent = new Intent(getApplicationContext(),
                        LoginActivity.class);
                startActivity(intent);
            }
        });

        String userPoint = Integer.toString(getUser.getInt("userPoint",0));
        txtUser.setText(getUser.getString("userName","")+"님 환영합니다.");
        txtPhone.setText(getUser.getString("userPhone",""));
        txtEmail.setText(getUser.getString("userId",""));
        txtAddress.setText(getUser.getString("userAddress",""));
        txtPoint.setText(userPoint);
    }
}
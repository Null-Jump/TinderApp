package com.example.tinderapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private  val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    override fun onStart() {
        super.onStart()

        // auth 에 저장된 데이터가 없을 경우(로그인이 되어 있지 않은 경우)
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java)) //로그인 액티비티로 이동
        }else{
            startActivity(Intent(this, LikeActivity::class.java))
        }
    }
}
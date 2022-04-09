package com.example.tinderapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LikeActivity : AppCompatActivity() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child("Users")

        val currentUserDB = userDB.child(getCurrentUserID())
        // 값(Data)를 가져오기 위한 코드
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            //DB의 데이터가 수정되었을 때 실행되는 메소드
            override fun onDataChange(snapshot: DataSnapshot) {
                // 받아온 이름이 null인지 확인
                if (snapshot.child("name").value == null) {
                    showNameInputPopup()
                    return
                } else { // 유저정보를 갱신

                }

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun showNameInputPopup() {
        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Input your name") // 얼럿다이얼로그 제목
            .setView(editText) // 얼럿다이얼로그를 EditText 로 설정
            .setPositiveButton("Save") { _, _ -> // 인자가 2개인 람다
                if (editText.text.isEmpty()) { // editText 가 비어있다면 다시 얼럿다이얼로그를 호출
                    showNameInputPopup()
                } else { // editText 에 내용이 입력되었다면 해당 내용을 유저의 이름으로 설정
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false) // 취소를 하지 못하도록 설정
            .show()
    }

    private fun saveUserName(name: String){
        val userId = getCurrentUserID()
        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDB.updateChildren(user)
    }

    // 현재 User ID를 가져오기 위한 메소드
    private fun getCurrentUserID(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "Pleas try login again", Toast.LENGTH_SHORT).show()
            finish() // 로그인 되어 있지 않다면 다시 메인액티비티로 넘어가서 로그인 액티비티가 나오게 하기 위한 finish()
        }

        return auth.currentUser?.uid.orEmpty()
    }

}
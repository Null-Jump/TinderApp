package com.example.tinderapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    /** 페이스븍 로그인 버튼 클릭 -> 페이스북 앱 실행 -> 로그인 완료 후 다시 앱으로 리턴(ActivityCallBack 으로 넘어옴) -> onActivityResult()가 실행됨
    -> onActivityResult()에는 페이스북에서 가져온 값이 있고 LoginActivity에 그 값을 전달해서 로그인 처리 */
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        callbackManager = CallbackManager.Factory.create()

        initSighUpButton()
        initLoginButton()
        initEmailAndPasswordEditText()

        initFacebookLoginButton()
    }

    private fun initSighUpButton() {
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        signUpButton.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPassword()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Success Sign Up, you can use this app",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Fail Sign up, check your Email address",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun initLoginButton() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPassword()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        /**
                         * 로그인 액티비티는 메인 액티비티가 로그인 되어 있지 않을때 실행 되는 액티비티 로써
                         * 로그인이 성공적으로 이뤄졌다면 firebase.auth 에 저장이 될 것이고
                         * 로그인 액티비티의 역할은 끝났다고 볼 수 있으므로 해당 액티비티를 종료시킴
                         */
                        handleSuccessLogin()

                    } else {
                        Toast.makeText(
                            this,
                            "Fail Login, check your Email or password",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun initFacebookLoginButton() {
        val facebookLoginButton = findViewById<LoginButton>(R.id.facebookLoginButton)

        // Facebook 에 권한을 요청해서 가져올 정보의 종류를 특정
        facebookLoginButton.setReadPermissions("email", "public_profile")
        facebookLoginButton.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // 로그인 성공
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener(this@LoginActivity) { task ->
                            if (task.isSuccessful) {
                                handleSuccessLogin()
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Fail Login, check your Email or password",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }

                override fun onCancel() {
                    // 로그인 취소
                }

                override fun onError(error: FacebookException?) {
                    // 로그인 중 에러
                    Toast.makeText(
                        this@LoginActivity,
                        "Facebook Login is Fail.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun getInputEmail(): String {
        return findViewById<EditText>(R.id.emailEditText).text.toString()
    }

    private fun getInputPassword(): String {
        return findViewById<EditText>(R.id.passwordEditText).text.toString()
    }

    private fun initEmailAndPasswordEditText() {
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        emailEditText.addTextChangedListener { // EditText 에 text 가 입력될 때 마다 해당 리스너로 이벤트를 발생
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }

        passwordEditText.addTextChangedListener { // EditText 에 text 가 입력될 때 마다 해당 리스너로 이벤트를 발생
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleSuccessLogin(){
        if(auth.currentUser == null){
            Toast.makeText(this, "Login is fail", Toast.LENGTH_SHORT).show()
            return
        }
        /**
         * Firebase.database 안에 Users 라는 List 가 생성되고 그 안에
         * userId 라는 오브젝트가 생기고 그 안에
         * user 가 생성됨
         */
        val userId = auth.currentUser?.uid.orEmpty()
        val currentUserDB = Firebase.database.reference.child("Users").child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        currentUserDB.updateChildren(user)

        finish()
    }
}
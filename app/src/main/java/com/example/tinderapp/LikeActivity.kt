package com.example.tinderapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity(), CardStackListener {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference

    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()

    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child("Users")

        val currentUserDB = userDB.child(getCurrentUserID())
        // 값(Data)를 가져오기 위한 코드
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            //DB의 데이터가 수정되었을 때 실행되는 메소드
            override fun onDataChange(snapshot: DataSnapshot) {
                // 받아온 이름이 null 인지 확인
                if (snapshot.child("name").value == null) {
                    showNameInputPopup()
                    return
                }

                getUnSelectedUsers()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        initCardStackView()
    }

    private fun initCardStackView(){
        val stackView = findViewById<CardStackView>(R.id.cardStackView)

        stackView.layoutManager = manager
        stackView.adapter = adapter
    }

    private fun getUnSelectedUsers(){
        //addChildEventListener() -> 리스너를 불러와서 사용하기 때문에 userDB 안에 모든 변동사항이 해당 리스너로 떨어짐
        userDB.addChildEventListener(object: ChildEventListener{ // 다음 implement 들로 커스텀 가능함
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (// 불러온 ID 가 본 사용자와 같지 않아야함
                    snapshot.child("userId").value != getCurrentUserID()
                    // 본 사용자가 like 한 사용자인지 상대방 유저 정보에 추가되어있지 않아야함
                    && snapshot.child("likedBy").child("like").hasChild(getCurrentUserID()).not()
                    // 본 사용자가 disLike 한 사용자인지 상대방 유저 정보에 추가되어있지 않아야함
                    && snapshot.child("likedBy").child("disLike").hasChild(getCurrentUserID()).not()){

                    val userId = snapshot.child("userId").value.toString()
                    var name = "undecided"
                    if(snapshot.child("name").value != null){
                        name = snapshot.child("name").value.toString()
                    }

                    cardItems.add(CardItem(userId, name))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // DB에 저장된 데이터들중 사용자의 이름이 변경된 경우 변경한 이름을 반영하는 코드
                cardItems.find { it.userId == snapshot.key }?.let {
                    it.name = snapshot.child("name").value.toString()
                }

                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
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

        getUnSelectedUsers()
    }

    // 현재 User ID를 가져오기 위한 메소드
    private fun getCurrentUserID(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "Pleas try login again", Toast.LENGTH_SHORT).show()
            finish() // 로그인 되어 있지 않다면 다시 메인액티비티로 넘어가서 로그인 액티비티가 나오게 하기 위한 finish()
        }

        return auth.currentUser?.uid.orEmpty()
    }

    private fun like(){
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child("likeBy")
            .child("like")
            .child(getCurrentUserID())
            .setValue(true)

        saveMatchIfOtherUserLikedMe(card.userId)
        //Todo 매칭이 된 시점을 봐야함
        Toast.makeText(this, "you like${card.name}", Toast.LENGTH_SHORT).show()
    }

    private fun disLike(){
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child("likeBy")
            .child("disLike")
            .child(getCurrentUserID())
            .setValue(true)

        Toast.makeText(this, "you dislike ${card.name}", Toast.LENGTH_SHORT).show()
    }

    private fun saveMatchIfOtherUserLikedMe(otherUserId: String){
        val otherUserDB = userDB.child(getCurrentUserID()).child("likedBy").child("like").child(otherUserId)
        otherUserDB.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value == true){
                    // 현재 유저의 DB에 매치가 되었다고 저장
                    userDB.child(getCurrentUserID())
                        .child("likeBy")
                        .child("match")
                        .child(otherUserId)
                        .setValue(true)

                    // 상대방의 DB에 매치가 되었다고 저장
                    userDB.child(otherUserId)
                        .child("likeBy")
                        .child("match")
                        .child(getCurrentUserID())
                        .setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    /**
     * CardStackListener()를 사용하기 위한 implement 들로 사용할 기능들을 커스텀 할 수 있다
     */
    override fun onCardDragging(direction: Direction?, ratio: Float) {    }

    override fun onCardSwiped(direction: Direction?) {
        when (direction){
            Direction.Right -> like()
            Direction.Left -> disLike()
            else -> {}
        }
    }

    override fun onCardRewound() {    }

    override fun onCardCanceled() {    }

    override fun onCardAppeared(view: View?, position: Int) {    }

    override fun onCardDisappeared(view: View?, position: Int) {    }

}
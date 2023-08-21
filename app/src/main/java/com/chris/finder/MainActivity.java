package com.chris.finder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chris.finder.auth.Login;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> al, swipedLeftCards, swipedRightCards;
    private ArrayAdapter arrayAdapter;
    private int i;

    private SwipeFlingAdapterView flingContainer;
    private Button left, right, logoutBt, resetBt;

    private String friendId;

    FirebaseAuth auth;
    TextView userDetails, friendStatusTextView, matchTV;
    FirebaseUser user;

    private ValueEventListener friendStatusListener;
    private DatabaseReference usersRef, userRef, friendRef, leftRef, rightRef, matchesRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flingContainer = findViewById(R.id.frame);
        swipedLeftCards = new ArrayList<>();
        swipedRightCards = new ArrayList<>();

        left = findViewById(R.id.left);
        right = findViewById(R.id.right);
        resetBt = findViewById(R.id.reset);

        matchTV = findViewById(R.id.matchTV);

        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRightClick(v);
            }
        });

        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLeftClick(v);
            }
        });

        resetBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                al.clear();
                arrayAdapter.clear(); // Clear the adapter data
                al.add("php");
                al.add("c");
                al.add("python");
                al.add("java");
                al.add("html");
                al.add("c++");
                al.add("css");
                i = 0;

                arrayAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.item, R.id.helloText, al);
                flingContainer.setAdapter(arrayAdapter);
                arrayAdapter.notifyDataSetChanged();
            }
        });

        al = new ArrayList<>();
        al.add("php");
        al.add("c");
        al.add("python");
        al.add("java");
        al.add("html");
        al.add("c++");
        al.add("css");
        al.add("javascript");

        arrayAdapter = new ArrayAdapter<>(this, R.layout.item, R.id.helloText, al);
        matchesRef = FirebaseDatabase.getInstance().getReference("matches");

        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!");
                al.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {
                String swipedLeftCard = dataObject.toString();
                makeToast(MainActivity.this, swipedLeftCard);
                swipedLeftCards.add(swipedLeftCard);
                if (!swipedLeftCards.isEmpty()) {
                    leftRef = userRef.child("swipe").child("left");
                    leftRef.setValue(swipedLeftCards);
                }

                leftRef = matchesRef.child(swipedLeftCard).child(user.getUid());
                leftRef.setValue(false);

                checkForMatch(swipedLeftCard);
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                String swipedRightCard = dataObject.toString();
                makeToast(MainActivity.this, swipedRightCard);
                swipedRightCards.add(swipedRightCard);
                if (!swipedRightCards.isEmpty()) {
                    rightRef = userRef.child("swipe").child("right");
                    rightRef.setValue(swipedRightCards);
                }

                rightRef = matchesRef.child(swipedRightCard).child(user.getUid());
                rightRef.setValue(true);

                checkForMatch(swipedRightCard);
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                // Ask for more data here
                al.add("XML ".concat(String.valueOf(i)));
                arrayAdapter.notifyDataSetChanged();
                Log.d("LIST", "notified");
                i++;
            }

            @Override
            public void onScroll(float scrollProgressPercent) {
                View view = flingContainer.getSelectedView();
                view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
            }
        });

        auth = FirebaseAuth.getInstance();
        logoutBt = findViewById(R.id.logout);
        userDetails = findViewById(R.id.user_details);
        friendStatusTextView = findViewById(R.id.friend_status);
        user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        } else {
            userDetails.setText("Email: " + user.getEmail() + "Uid: " + user.getUid());

            usersRef = FirebaseDatabase.getInstance().getReference("users");
            userRef = usersRef.child(user.getUid());

            auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if (firebaseAuth.getCurrentUser() != null) {
                        userRef.child("online").setValue(true);
                        userRef.child("online").onDisconnect().setValue(false);
                    }
                }
            });

            friendRef = userRef.child("friend");
            friendStatusListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String friendId = snapshot.getValue(String.class);

                    if (friendId != null) {
                        DatabaseReference friendStatusRef = usersRef.child(friendId).child("online");
                        friendStatusRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Boolean friendOnlineStatus = snapshot.getValue(Boolean.class);
                                String status;

                                if (friendOnlineStatus != null) {
                                    status = friendOnlineStatus ? "Friend is Online" : "Friend is Offline";
                                } else {
                                    status = "No data";
                                }

                                friendStatusTextView.setText(status);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle onCancelled if needed
                            }
                        });
                    } else {
                        // Handle the case where there is no friend
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle onCancelled if needed
                }
            };

            friendRef.addValueEventListener(friendStatusListener);


        }

        logoutBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(v.getContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });
    }

    static void makeToast(Context ctx, String s) {
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
    }

    public void onRightClick(View view) {
        flingContainer.getTopCardListener().selectRight();
    }

    public void onLeftClick(View view) {
        flingContainer.getTopCardListener().selectLeft();
    }

    private void checkForMatch(String cardId) {
        DatabaseReference matchedCardRef = matchesRef.child(cardId);

        userRef.child("friend").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendId = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        matchedCardRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean currentUserSwipedRight = snapshot.child(user.getUid()).getValue(Boolean.class);
                Boolean otherUserSwipedRight = snapshot.child(friendId).getValue(Boolean.class);

                if (currentUserSwipedRight != null && otherUserSwipedRight != null) {
                    if (currentUserSwipedRight && otherUserSwipedRight) {
                        // Both users have swiped right, it's a match!
                        matchTV.setText("Match! You both swiped on " + snapshot.getKey());
                        matchTV.setVisibility(View.VISIBLE);

                    } else {
                        // Not a match
                        matchTV.setText("");
                        matchTV.setVisibility(View.GONE);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendRef != null && friendStatusListener != null) {
            friendRef.removeEventListener(friendStatusListener);
        }
    }
}
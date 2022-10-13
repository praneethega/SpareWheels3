package com.example.sparewheels3.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sparewheels3.Constants;
import com.example.sparewheels3.R;
import com.example.sparewheels3.adapters.AdapterCartItem;
import com.example.sparewheels3.adapters.AdapterProductUser;
import com.example.sparewheels3.models.ModelCartItem;
import com.example.sparewheels3.models.ModelProduct;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class ShopDetailsActivity extends AppCompatActivity {


    public String deliveryFee;
    private ImageView shopIv;
    private TextView shopNameTv,phoneTv,emailTv,openCloseTv,deliveryFeeTv,addressTv,filteredProductsTv,cartCountTv;
    private ImageButton callBtn,mapBtn,cartBtn,backBtn,filterProductBtn;
    private EditText searchProductEt;
    private RecyclerView productsRv;

    private String shopUid;
    private String myLatitude,myLongitude,myPhone;
    private String shopName,shopEmail,shopPhone,shopAddress,shopLatitude,shopLongitude;



    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private ArrayList<ModelProduct> productsList;
    private AdapterProductUser adapterProductUser;

    private ArrayList<ModelCartItem> cartItemsList;
    private AdapterCartItem adapterCartItem;

    private EasyDB easyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);

        shopIv=findViewById(R.id.shopIv);
        shopNameTv=findViewById(R.id.shopNameTv);
        phoneTv=findViewById(R.id.phoneTv);
        emailTv=findViewById(R.id.emailTv);
        openCloseTv=findViewById(R.id.openCloseTv);
        deliveryFeeTv=findViewById(R.id.deliveryFeeTv);
        addressTv=findViewById(R.id.addressTv);
        filteredProductsTv=findViewById(R.id.filteredProductsTv);
        callBtn=findViewById(R.id.callBtn);
        mapBtn=findViewById(R.id.mapBtn);
        cartBtn=findViewById(R.id.cartBtn);
        backBtn=findViewById(R.id.backBtn);
        filterProductBtn=findViewById(R.id.filterProductBtn);
        searchProductEt=findViewById(R.id.searchProductEt);
        productsRv=findViewById(R.id.productsRv);
        cartCountTv=findViewById(R.id.cartCountTv);

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        shopUid=getIntent().getStringExtra("shopUid");
        firebaseAuth=FirebaseAuth.getInstance();
        loadMyInfo();
        loadShopDetails();
        loadShopProducts();

        easyDB=EasyDB.init(this,"ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id",new String[]{"text","unique"}))
                .addColumn(new Column("Item_PID",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Name",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price_Each",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Quantity",new String[]{"text","not null"}))
                .doneTableColumn();

        deleteCartData();
        cartCount();

        searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try{
                    adapterProductUser.getFilter().filter(s);
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });



        cartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCartDialog();

            }
        });

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialPhone();
            }
        });

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        filterProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder=new AlertDialog.Builder(ShopDetailsActivity.this);
                builder.setTitle("Choose Category:")
                        .setItems(Constants.productCategories1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String selected =Constants.productCategories1[which];
                                filteredProductsTv.setText(selected);
                                if(selected.equals("All")){
                                    loadShopProducts();
                                }
                                else{
                                    adapterProductUser.getFilter().filter(selected);
                                }
                            }
                        })
                        .show();
            }
        });

    }

    private void deleteCartData() {
        easyDB.deleteAllDataFromTable();
    }

    public void cartCount(){
        int count =easyDB.getAllData().getCount();
        if (count<=0){
            cartCountTv.setVisibility(View.GONE);
        }
        else{
            cartCountTv.setVisibility(View.VISIBLE);
            cartCountTv.setText(""+count);
        }
    }
    public double allTotalPrice=0.00;
    public TextView sTotalTv,dFeeTv,allTotalPriceTv;
    private void showCartDialog() {

        cartItemsList = new ArrayList<ModelCartItem>();

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cart,null);

        TextView shopNameTv =view.findViewById(R.id.shopNameTv);
        RecyclerView cartItemsRv=view.findViewById(R.id.cartItemsRv);
        sTotalTv=view.findViewById(R.id.sTotalTv);
        dFeeTv=view.findViewById(R.id.dFeeTv);
        allTotalPriceTv=view.findViewById(R.id.totalTv);
        Button checkoutBtn=view.findViewById(R.id.checkoutBtn);

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setView(view);
        shopNameTv.setText(shopName);

        EasyDB easyDB=EasyDB.init(this,"ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id",new String[]{"text","unique"}))
                .addColumn(new Column("Item_PID",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Name",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price_Each",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Quantity",new String[]{"text","not null"}))
                .doneTableColumn();

        Cursor res=easyDB.getAllData();
        while(res.moveToNext()){
            String id=res.getString(1);
            String pId=res.getString(2);
            String name=res.getString(3);
            String price=res.getString(4);
            String cost=res.getString(5);
            String quantity=res.getString(6);

            allTotalPrice = allTotalPrice+Double.parseDouble(cost);

            ModelCartItem modelCartItem=new ModelCartItem(
                    ""+id,
                    ""+pId,
                    ""+name,
                    ""+price,
                    ""+cost,
                    ""+quantity
            );
            cartItemsList.add(modelCartItem);

        }


        adapterCartItem=new AdapterCartItem(this,cartItemsList);

        cartItemsRv.setAdapter(adapterCartItem);
        dFeeTv.setText("Rs."+deliveryFee);
        sTotalTv.setText("Rs."+String.format("%.2f",allTotalPrice));
        allTotalPriceTv.setText("Rs."+(allTotalPrice+Double.parseDouble(deliveryFee.replace("Rs.",""))));

        AlertDialog dialog =builder.create();
        dialog.show();

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                allTotalPrice=0.00;

            }
        });

        checkoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myLatitude.equals("")||myLatitude.equals("null")||myLongitude.equals("")||myLongitude.equals("null")){
                    Toast.makeText(ShopDetailsActivity.this, "Please Enter Valid Address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(myPhone.equals("")||myPhone.equals("null")){
                    Toast.makeText(ShopDetailsActivity.this, "Please Enter Valid Mobile Number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(cartItemsList.size()==0){
                    Toast.makeText(ShopDetailsActivity.this, "Cart is Empty", Toast.LENGTH_SHORT).show();
                }

                submitOrder();
            }
        });

    }

    private void submitOrder() {
        progressDialog.setMessage("Placing Order..");
        progressDialog.show();

        String timestamp=""+System.currentTimeMillis();

        String cost=allTotalPriceTv.getText().toString().trim().replace("Rs.","");

        HashMap<String,String> hashMap=new HashMap<>();

        hashMap.put("orderId",""+timestamp);
        hashMap.put("orderTime",""+timestamp);
        hashMap.put("orderStatus","In Progress");
        hashMap.put("orderCost",""+cost);
        hashMap.put("orderBy",""+firebaseAuth.getUid());
        hashMap.put("orderTo",""+shopUid);
        hashMap.put("latitude",""+myLatitude);
        hashMap.put("longitude",""+myLongitude);

        final DatabaseReference ref =FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        for (int i = 0; i < cartItemsList.size(); i++) {
                            String pId = cartItemsList.get(i).getpId();
                            String id = cartItemsList.get(i).getId();
                            String cost = cartItemsList.get(i).getCost();
                            String name = cartItemsList.get(i).getName();
                            String price = cartItemsList.get(i).getPrice();
                            String quantity = cartItemsList.get(i).getQuantity();

                            HashMap<String, String> hashMap1 = new HashMap<>();
                            hashMap1.put("pId", pId);
                            hashMap1.put("name", name);
                            hashMap1.put("cost", cost);
                            hashMap1.put("price", price);
                            hashMap1.put("quantity", quantity);

                            ref.child(timestamp).child("Items").child(pId).setValue(hashMap1);

                        }
                        progressDialog.dismiss();
                        Toast.makeText(ShopDetailsActivity.this, "Order Placed", Toast.LENGTH_SHORT).show();

                        Intent intent=new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
                        intent.putExtra("orderTo",shopUid);
                        intent.putExtra("orderId",timestamp);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(ShopDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void openMap() {
        String address ="https://maps.google.com/maps?saddr="+myLatitude+","+myLongitude+"&daddr="+shopLatitude+","+shopLongitude;
        Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(address));
        startActivity(intent);
    }
    private void dialPhone() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+Uri.encode(shopPhone))));
        Toast.makeText(this, ""+shopPhone, Toast.LENGTH_SHORT).show();
    }




    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            String name =""+ds.child("name").getValue();
                            String email =""+ds.child("email").getValue();
                            myPhone =""+ds.child("phone").getValue();
                            String profileImage =""+ds.child("profileImage").getValue();
                            String accountType =""+ds.child("accountType").getValue();
                            String city =""+ds.child("city").getValue();
                            myLatitude=""+ds.child("latitude").getValue();
                            myLongitude=""+ds.child("longitude").getValue();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
    private void loadShopDetails() {
        DatabaseReference ref =FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String name=""+dataSnapshot.child("name").getValue();
                shopName=""+dataSnapshot.child("shopName").getValue();
                shopEmail=""+dataSnapshot.child("email").getValue();
                shopPhone=""+dataSnapshot.child("phone").getValue();
                shopLatitude=""+dataSnapshot.child("latitude").getValue();
                shopAddress=""+dataSnapshot.child("address").getValue();
                shopLongitude=""+dataSnapshot.child("longitude").getValue();
                deliveryFee=""+dataSnapshot.child("deliveryFee").getValue();
                String profileImage=""+dataSnapshot.child("profileImage").getValue();
                String shopOpen=""+dataSnapshot.child("shopOpen").getValue();

                shopNameTv.setText(shopName);
                emailTv.setText(shopEmail);
                deliveryFeeTv.setText("Delivery Fee:Rs."+deliveryFee);
                addressTv.setText(shopAddress);
                phoneTv.setText(shopPhone);
                if(shopOpen.equals("true")){
                    openCloseTv.setText("We're Open Now!!");
                }
                else{
                    openCloseTv.setText("We're Closed Now!!");
                }
                try{
                    Picasso.get().load(profileImage).into(shopIv);
                }
                catch(Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void loadShopProducts() {
        productsList = new ArrayList<>();

        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        productsList.clear();
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            ModelProduct modelProduct=ds.getValue(ModelProduct.class);
                            productsList.add(modelProduct);
                        }
                        adapterProductUser=new AdapterProductUser(ShopDetailsActivity.this,productsList);
                        productsRv.setAdapter(adapterProductUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }




}
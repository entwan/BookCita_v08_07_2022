package com.dam.bookcita.activity;

import static com.dam.bookcita.common.Constantes.*;
import static com.google.firebase.firestore.FieldPath.documentId;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dam.bookcita.R;
import com.dam.bookcita.adapter.AdapterBookNbrCitations;
import com.dam.bookcita.adapter.AdapterCitation;
import com.dam.bookcita.adapter.AdapterDetailsBook;
import com.dam.bookcita.fragment.MesCitationsFragment;
import com.dam.bookcita.model.ModelCitation;
import com.dam.bookcita.model.ModelDetailsLivre;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ListeCitationsFromOneBookActivity extends AppCompatActivity implements AdapterCitation.OnItemClickListener {

    private static final String TAG = "ListeCitationsFromOneBo";

    private TextView tvTitreLC;
    private TextView tvAuteurLC;
    private ImageView ivCoverLC;
    private TextView tvNbCitationsLC;
    private RecyclerView rvCitationsFOB;

    private AdapterCitation adapterCitation;

    private ProgressDialog progressDialog;

    private String id_BD;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference citationsRef = db.collection("citations");
    private FirebaseAuth auth;

    private String id_user;


    private void init(){
        tvTitreLC = findViewById(R.id.tvTitreLC);
        tvAuteurLC = findViewById(R.id.tvAuteurLC);
        ivCoverLC = findViewById(R.id.ivCoverLC);
        tvNbCitationsLC = findViewById(R.id.tvNbCitationsLC);
        rvCitationsFOB = findViewById(R.id.rvCitationsFOB);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liste_citations_from_one_book);
        Intent intent = getIntent();
        id_BD = intent.getStringExtra(ID_BD);


        init();

        getFicheBookFromDB();

        auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();

        id_user = firebaseUser.getUid();

        rvCitationsFOB.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false));

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Fetching Data...");
        progressDialog.show();

        getCitationsFOBFromDB();
        remplirNbCitationsFOBFromDB();
    }

    public void getCitationsFOBFromDB(){
        Query query = citationsRef
                .whereEqualTo("id_user",id_user)
                .whereEqualTo("id_BD_livre",id_BD);
//        Query query = citationsRef.orderBy("citation");
        Log.i(TAG, "Query : "+query);
        FirestoreRecyclerOptions<ModelCitation> citationsFOB = new FirestoreRecyclerOptions.Builder<ModelCitation>()
                .setQuery(query, ModelCitation.class)
                .build();
        adapterCitation = new AdapterCitation(citationsFOB);
        rvCitationsFOB.setAdapter(adapterCitation);
        adapterCitation.setOnItemClickListener(ListeCitationsFromOneBookActivity.this);
        if (progressDialog.isShowing()){
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapterCitation.startListening();
    }

    private void getFicheBookFromDB() {


//        Query query = livresRef.whereEqualTo("id", id_BD);

        db.collection("livres")
                .whereEqualTo(documentId(), id_BD)
//                .whereEqualTo("auteur_livre", "Luc Lang")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            //comme on filtre par id, on devrait avoir ici qu'un seul resultat
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.i(TAG, document.getId() + " => " + document.getData());
                                String titre = document.getString("title_livre");
                                String auteur = document.getString("auteur_livre");
                                String coverUrl = document.getString("url_cover_livre");
                                Log.i(TAG, "onComplete: titre : " + titre);
                                Log.i(TAG, "onComplete: auteur : " + auteur);
                                Log.i(TAG, "onComplete: coverUrl : " + coverUrl);
                                tvTitreLC.setText(titre);
                                tvAuteurLC.setText(auteur);
                                //Gestion de l'image avec Glide
                                Context context = ListeCitationsFromOneBookActivity.this;

                                RequestOptions options = new RequestOptions()
                                        .centerCrop()
                                        .error(R.drawable.ic_couverture_livre_150)
                                        .placeholder(R.drawable.ic_couverture_livre_150);

                                // methode normale
                                Glide.with(context)
                                        .load(coverUrl)
                                        .apply(options)
                                        .fitCenter()
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(ivCoverLC);
                            }
                        } else {
                            Log.i(TAG, "Error getting documents: ", task.getException());
                        }
                    }

                });


    }

    private void remplirNbCitationsFOBFromDB() {

        db.collection("citations")
                .whereEqualTo("id_user",id_user)
                .whereEqualTo("id_BD_livre", id_BD)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            int nbCitationFOB = querySnapshot.size();
                            Log.i(TAG, "onComplete: nbCitationFOB : " + nbCitationFOB);
                            tvNbCitationsLC.setText(String.valueOf(nbCitationFOB));
                        } else {
                            Log.i(TAG, "Error getting documents: ", task.getException());
                        }
                    }

                });


    }

    @Override
    public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
        Intent detailsCitationFOBIntent = new Intent(ListeCitationsFromOneBookActivity.this, DetailsCitationActivity.class);
        String id_citationSelectionnee = documentSnapshot.getId();
        Log.i(TAG, "onItemClick: id_citationSelectionnee : " + id_citationSelectionnee);
        detailsCitationFOBIntent.putExtra(ID_BD, id_BD);
        detailsCitationFOBIntent.putExtra(ID_BD_CITATION, id_citationSelectionnee);


        startActivity(detailsCitationFOBIntent);
    }
}
package br.com.bflm.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import br.com.bflm.fatec_ipi_noite_pdm_firebase_auth_firestore.R;

import static br.com.bflm.gallery.NovoUsuarioActivity.REQ_CODE_CAMERA;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mensagensRecyclerView;
    private ChatAdapter adapter;
    private List <Mensagem> mensagens;
    private FirebaseUser fireUser;
    private CollectionReference collMensagensReference;

    private EditText mensagemEditText;

    private void setupFirebase (){
        fireUser = FirebaseAuth.getInstance().getCurrentUser();
        collMensagensReference =
                FirebaseFirestore.
                        getInstance().
                        collection("mensagens");

        collMensagensReference.addSnapshotListener((result, e) -> {
           mensagens.clear();
           for (DocumentSnapshot doc : result.getDocuments()){
               Mensagem m = doc.toObject(Mensagem.class);
               mensagens.add(m);
           }
           Collections.sort(mensagens);
           adapter.notifyDataSetChanged();
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        setupFirebase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mensagemEditText =
                findViewById(R.id.mensagemEditText);
        mensagensRecyclerView =
                findViewById(R.id.mensagensRecyclerView);
        mensagens = new ArrayList<>();
        adapter = new ChatAdapter(this, mensagens);
        LinearLayoutManager llm =
                new LinearLayoutManager(this);
        mensagensRecyclerView.setAdapter(adapter);
        mensagensRecyclerView.setLayoutManager(llm);
    }

    public void enviarMensagem(View view) {
        String texto =
                mensagemEditText.getText().toString();
        Mensagem m =
                new Mensagem (texto,
                        new java.util.Date(), fireUser.getEmail());
        m.setIsImage(false);
        collMensagensReference.add(m);
        mensagemEditText.setText("");
    }

    public void enviarFoto (View view){
            Intent intent = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
            //dá pra tirar foto?
            if (intent.resolveActivity(getPackageManager()) != null){
                startActivityForResult(intent, REQ_CODE_CAMERA);
            }
            else{
                Toast.makeText(
                        this,
                        getString(R.string.cant_take_pic),
                        Toast.LENGTH_SHORT
                ).show();
            }
    }

    private void uploadPicture (Bitmap picture){
        Mensagem m =
                new Mensagem ("",
                        new java.util.Date(), fireUser.getEmail());
        m.setIsImage(true);
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 20;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();
        m.setLinkImage(generatedString);
        StorageReference pictureStorageReference =
                FirebaseStorage.
                        getInstance().
                        getReference(
                                String.format(
                                        Locale.getDefault(),
                                        "imagessent/%s.jpg",
                                        m.getLinkImage()
                                )
                        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        picture.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte [] bytes = baos.toByteArray();
        //aqui foi feito o upload
        pictureStorageReference.putBytes(bytes);
        collMensagensReference.add(m);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data) {

        if (requestCode == REQ_CODE_CAMERA){
            if (resultCode == Activity.RESULT_OK){
                Bitmap picture = (Bitmap)
                        data.getExtras().get("data");
                uploadPicture(picture);
            }
            else{
                Toast.makeText(this,
                        getString(R.string.no_pic_taken),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
class ChatViewHolder extends RecyclerView.ViewHolder{
    public TextView dataNomeTextView;
    public TextView mensagemTextView;
    public ImageView profilePicImageView;
    public ImageView sendPicImageView;
    public ChatViewHolder (View raiz){
        super (raiz);
        dataNomeTextView =
                raiz.findViewById(R.id.dataNomeTextView);
        mensagemTextView =
                raiz.findViewById(R.id.mensagemTextView);
        profilePicImageView =
                raiz.findViewById(R.id.profilePicImageView);
        sendPicImageView =
                raiz.findViewById(R.id.sendPicImageView);
    }
}

class ChatAdapter extends RecyclerView.Adapter <ChatViewHolder>{

    private Context context;
    private List <Mensagem> mensagens;
    private Map <String, Bitmap> fotos;
    private Map <String, Bitmap> fotos2;

    public ChatAdapter(Context context, List<Mensagem> mensagens){
        this.context = context;
        this.mensagens = mensagens;
        fotos = new HashMap<>();
        fotos2 = new HashMap<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //return null;
        LayoutInflater inflater = LayoutInflater.from(context);
        View raiz = inflater.inflate(
            R.layout.list_item,
            parent,
            false
        );
        return new ChatViewHolder(raiz);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Mensagem m = mensagens.get(position);
        holder.sendPicImageView.setVisibility(View.GONE);
        holder.mensagemTextView.setVisibility(View.VISIBLE);
        if(m.getIsImage()){
            holder.sendPicImageView.setVisibility(View.VISIBLE);
            holder.mensagemTextView.setVisibility(View.GONE);
            StorageReference pictureStorageReference2 =
                    FirebaseStorage.getInstance().getReference(
                            String.format(
                                    Locale.getDefault(),
                                    "imagessent/%s.jpg",
                                    m.getLinkImage()
                            )
                    );
            if (fotos2.containsKey(m.getLinkImage())){
                holder.sendPicImageView.setImageBitmap(
                        fotos2.get(m.getLinkImage())
                );
            }
            else{
                pictureStorageReference2.getDownloadUrl()
                        .addOnSuccessListener(
                                (result) ->{
                                    Glide.
                                            with(context).
                                            asBitmap().addListener(new RequestListener<Bitmap>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                            fotos2.put(m.getLinkImage(), resource);
                                            holder.sendPicImageView.setImageBitmap(resource);
                                            return true;
                                        }
                                    }).
                                            load(pictureStorageReference2).
                                            into(holder.sendPicImageView);
                                }
                        )
                        .addOnFailureListener(
                                (exception) -> {
                                    holder.sendPicImageView.setImageResource(
                                            R.drawable.ic_person_black_50dp
                                    );
                                }
                        );
            }
        }

        holder.dataNomeTextView.setText(
                context.getString(
                        R.string.data_nome,
                        DateHelper.format(m.getData()),
                        m.getEmail()
                )
        );
        holder.mensagemTextView.setText(m.getTexto());

        StorageReference pictureStorageReference =
                FirebaseStorage.getInstance().getReference(
                    String.format(
                            Locale.getDefault(),
                            "images/%s/profilePic.jpg",
                            m.getEmail().replace("@", "")
                    )
                );

        if (fotos.containsKey(m.getEmail())){
            holder.profilePicImageView.setImageBitmap(
                    fotos.get(m.getEmail())
            );
        }
        else{
            pictureStorageReference.getDownloadUrl()
                    .addOnSuccessListener(
                            (result) ->{
                                Glide.
                                        with(context).
                                        asBitmap().addListener(new RequestListener<Bitmap>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                        fotos.put(m.getEmail(), resource);
                                        holder.profilePicImageView.setImageBitmap(resource);
                                        return true;
                                    }
                                }).
                                        load(pictureStorageReference).
                                        into(holder.profilePicImageView);
                            }
                    )
                    .addOnFailureListener(
                            (exception) -> {
                                holder.profilePicImageView.setImageResource(
                                        R.drawable.ic_person_black_50dp
                                );
                            }
                    );

        }

    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }
}


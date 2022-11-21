package com.zerobudget.bookito.ui.inbox;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.zerobudget.bookito.R;
import com.zerobudget.bookito.models.Requests.RequestModel;
import com.zerobudget.bookito.models.Requests.RequestTradeModel;
import com.zerobudget.bookito.ui.search.SearchResultsModel;

import java.util.ArrayList;

public class BookTrade_RecycleViewAdapter extends RecyclerView.Adapter<BookTrade_RecycleViewAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<SearchResultsModel> results;
    private RequestTradeModel requestTradeModel;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;

    private boolean exists;

    FirebaseFirestore db;
    FirebaseAuth auth;

    public BookTrade_RecycleViewAdapter(Context context, ArrayList<SearchResultsModel> bookModels, RequestTradeModel requestTradeModel) {
        this.context = context;
        this.results = bookModels;
        this.requestTradeModel = requestTradeModel;
        this.exists = false;

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }


    @NonNull
    @Override
    public BookTrade_RecycleViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.recycleview_library, parent, false);

        return new BookTrade_RecycleViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookTrade_RecycleViewAdapter.ViewHolder holder, int position) {
        Picasso.get().load(results.get(position).getBook().getThumbnail()).into(holder.thumbnail);
        holder.title.setText(results.get(position).getBook().getTitle());
        holder.author.setText(results.get(position).getBook().getAuthor());
        holder.bookmark_outline.setColorFilter(context.getColor(R.color.bookmark_outline_scambio), PorterDuff.Mode.SRC_ATOP);
        holder.bookmark.setColorFilter(context.getColor(R.color.bookmark_scambio), PorterDuff.Mode.SRC_ATOP);

        holder.book_selected.setOnClickListener(view -> {
            createNewSelectPopup(position, holder);
            //passaggio dei dati del new book al prossimo fragment
/*
            Bundle args = new Bundle();
            String bookString = Utils.getGsonParser().toJson(bookModels.get(position));
            args.putString("BK", bookString);
*/

            // Navigation.findNavController(holder.itemView).navigate(R.id.action_navigation_library_to_bookDeleteFragment, args);
        });
    }

    private void createNewSelectPopup(int position, BookTrade_RecycleViewAdapter.ViewHolder holder) {
        checkIfStillExists(requestTradeModel);

        dialogBuilder = new MaterialAlertDialogBuilder(context);

        View view = View.inflate(context, R.layout.popup_trade_book, null);

        TextView bookTitle = view.findViewById(R.id.book_title);
        TextView bookAuthor = view.findViewById(R.id.book_author);
        TextView bookDescription = view.findViewById(R.id.book_description);
        TextView bookOwner = view.findViewById(R.id.book_owner);

        Button tradeBtn = view.findViewById(R.id.btn_trade);
        Button annullaBtn = view.findViewById(R.id.btn_annulla);


        ImageView bookThumbnail = view.findViewById(R.id.book_thumbnail);
        ImageView bookmark = view.findViewById(R.id.bookmark);
        ImageView bookmarkOutline = view.findViewById(R.id.bookmark_outline);

        bookTitle.setText(results.get(holder.getAdapterPosition()).getBook().getTitle());
        bookAuthor.setText(results.get(holder.getAdapterPosition()).getBook().getAuthor());
        bookDescription.setText(results.get(holder.getAdapterPosition()).getBook().getDescription());
        bookDescription.setMovementMethod(new ScrollingMovementMethod());

        String owner = results.get(holder.getAdapterPosition()).getUser().getFirst_name() + " " + results.get(holder.getAdapterPosition()).getUser().getLast_name();
        bookOwner.setText(owner);
        Picasso.get().load(results.get(holder.getAdapterPosition()).getBook().getThumbnail()).into(bookThumbnail);

        switch (results.get(holder.getAdapterPosition()).getBook().getType()) {
            case "Scambio":
                bookmarkOutline.setColorFilter(context.getColor(R.color.bookmark_outline_scambio), PorterDuff.Mode.SRC_ATOP);
                bookmark.setColorFilter(context.getColor(R.color.bookmark_scambio), PorterDuff.Mode.SRC_ATOP);
                break;
            case "Prestito":
                bookmarkOutline.setColorFilter(context.getColor(R.color.bookmark_outline_prestito), PorterDuff.Mode.SRC_ATOP);
                bookmark.setColorFilter(context.getColor(R.color.bookmark_prestito), PorterDuff.Mode.SRC_ATOP);
                break;
            case "Regalo":
                bookmarkOutline.setColorFilter(context.getColor(R.color.bookmark_outine_regalo), PorterDuff.Mode.SRC_ATOP);
                bookmark.setColorFilter(context.getColor(R.color.bookmark_regalo), PorterDuff.Mode.SRC_ATOP);
                break;
            default:
                Picasso.get().load(R.drawable.bookmark_template).into(bookmark);
                break;
        }


        tradeBtn.setOnClickListener(view1 -> {
            if(exists) { //controlla che la richiesta esista ancora
                acceptRequest(requestTradeModel, results.get(holder.getAdapterPosition()).getBook().getIsbn());
                Toast.makeText(context, "Richiesta accettata!", Toast.LENGTH_LONG).show();
                Navigation.findNavController(holder.itemView).navigate(R.id.action_bookTradeFragment_to_request_page_nav);
            }else{
                Toast.makeText(context, "Oh no, la richiesta è stata annullata!", Toast.LENGTH_LONG).show();
                Navigation.findNavController(holder.itemView).navigate(R.id.action_bookTradeFragment_to_request_page_nav);
            }
            dialog.dismiss();
        });

        annullaBtn.setOnClickListener(view2 -> {
            Toast.makeText(context, "Devi scegliere un libro!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        dialogBuilder.setView(view);
        dialog = dialogBuilder.create();
        dialog.show();
    }

    protected void acceptRequest(RequestTradeModel r, String isbnTradeBk) {
        db.collection("requests").document(r.getrequestId()).update("status", "accepted");
        db.collection("requests").document(r.getrequestId()).update("requestTradeBook", isbnTradeBk);
    }

    private void checkIfStillExists(RequestTradeModel r){
        db.collection("requests").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        if(doc.getId().equals(r.getrequestId())) {
                            exists = true;
                        }
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Layout
        private final ConstraintLayout book_selected;
        private final ImageView thumbnail;
        private final TextView title;
        private final TextView author;
        private final ImageView bookmark;
        private final ImageView bookmark_outline;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Binding from xml layout to Class
            book_selected = itemView.findViewById(R.id.book);
            thumbnail = itemView.findViewById(R.id.book_thumbnail);
            title = itemView.findViewById(R.id.book_title);
            author = itemView.findViewById(R.id.book_author);
            bookmark = itemView.findViewById(R.id.bookmark);
            bookmark_outline = itemView.findViewById(R.id.bookmark_outline);
        }
    }
}

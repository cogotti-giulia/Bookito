package com.zerobudget.bookito.ui.search;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;
import com.zerobudget.bookito.Notifications;
import com.zerobudget.bookito.R;
import com.zerobudget.bookito.models.Requests.RequestModel;
import com.zerobudget.bookito.models.Requests.RequestShareModel;
import com.zerobudget.bookito.utils.Utils;
import com.zerobudget.bookito.utils.popups.PopupSearchBook;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Search_RecycleViewAdapter extends RecyclerView.Adapter<Search_RecycleViewAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<SearchResultsModel> results;

    private final FirebaseFirestore db;
    FirebaseAuth auth;

    private AlertDialog dialog;

    public Search_RecycleViewAdapter(Context context, ArrayList<SearchResultsModel> bookModels) {
        this.context = context;
        this.results = bookModels;

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public Search_RecycleViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.recycleview_search_results, parent, false);

        return new Search_RecycleViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Search_RecycleViewAdapter.ViewHolder holder, int position) {
        Picasso.get().load(results.get(position).getBook().getThumbnail()).into(holder.thumbnail);
        holder.title.setText(results.get(position).getBook().getTitle());
        holder.author.setText(results.get(position).getBook().getAuthor());
        String owner = results.get(position).getUser().getFirstName() + " " + results.get(position).getUser().getLastName();
        holder.book_owner.setText(owner);
        holder.neighborhood_owner.setText(context.getString(R.string.user_location, results.get(position).getUser().getTownship(), results.get(position).getUser().getCity()));
        //holder.type.setText(results.get(position).getBook().getType());


        switch (results.get(holder.getAdapterPosition()).getBook().getType()) {
            case "Scambio":
                Picasso.get().load(R.drawable.swap).into(holder.book_type);
                break;
            case "Prestito":
                Picasso.get().load(R.drawable.calendar).into(holder.book_type);
                break;
            case "Regalo":
                Picasso.get().load(R.drawable.gift).into(holder.book_type);
                break;
            default:
                break;
        }
        holder.book_selected.setOnClickListener(view -> {
            createNewSearchPopup(holder);

            /*Bundle args = new Bundle();
            String usrBookString = Utils.getGsonParser().toJson(results.get(position));
            args.putString("USR_BK", usrBookString);*/

            //Navigation.findNavController(holder.itemView).navigate(R.id.action_navigation_search_to_bookRequestFragment, args);

        });
    }

    /**
     * crea il popup per la richiesta del libro, utilizzando la classe PopupSearchBook che
     * eredita alcuni metodi dal PopupBook ed evita la ripetizione di righe di codice*/
    private void createNewSearchPopup(ViewHolder holder) {
        View view = View.inflate(context, R.layout.popup_book, null);
        PopupSearchBook dialogBuilder = new PopupSearchBook(context, view);

        dialogBuilder.setUpInformation(results.get(holder.getAdapterPosition()));
        dialogBuilder.getBtnOther().setVisibility(View.GONE);
        dialogBuilder.setTextBtnDefault("Richiedi");

        dialogBuilder.getBtnDefault().setOnClickListener(view1 -> {
            String type = results.get(holder.getAdapterPosition()).getBook().getType();
            //preleva l'id dell'utente dal database
            db.collection("users").get().addOnCompleteListener(task -> {
                RequestModel rm;
                if (type.equals("Prestito"))
                    rm = new RequestShareModel();
                else rm = new RequestModel();

                rm.setRequestedBook(results.get(holder.getAdapterPosition()).getBook().getIsbn());
                rm.setTitle(results.get(holder.getAdapterPosition()).getBook().getTitle());
                rm.setThumbnail(results.get(holder.getAdapterPosition()).getBook().getThumbnail());
                rm.setStatus("undefined");
                rm.setType(results.get(holder.getAdapterPosition()).getBook().getType());
                rm.setSender(Utils.USER_ID);
                rm.setNote(dialogBuilder.getTxtRequestNote().getText().toString());

                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        if (doc.get("telephone").equals(results.get(holder.getAdapterPosition()).getUser().getTelephone())) {
                            rm.setReceiver(doc.getId());
                            Log.d("REC", rm.getReceiver());

                            if (rm instanceof RequestShareModel)
                                openCalendarPopup((RequestShareModel) rm, holder, dialog);
                            else
                                requestBook(rm, holder, dialog); //prova a inserire la richiesta del libro
                        }
                    }
                }
            });

        });

        dialogBuilder.setView(view);
        dialog = dialogBuilder.create();
        dialog.show();
    }

    private void openCalendarPopup(RequestShareModel rm, ViewHolder holder, AlertDialog dialog) {
        MaterialAlertDialogBuilder calendarPopup = new MaterialAlertDialogBuilder(context);
        View popup_view = View.inflate(context, R.layout.popup_datepicker, null);

        DatePicker datePicker = popup_view.findViewById(R.id.date_picker);
        datePicker.setMinDate(System.currentTimeMillis());

        calendarPopup.setView(popup_view);
        AlertDialog builderDate = calendarPopup.create();

        Button acceptButton = popup_view.findViewById(R.id.acceptButton);
        Button refuseButton = popup_view.findViewById(R.id.refuseButton);

        acceptButton.setOnClickListener(click -> {
            Calendar calendar = new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            rm.setDate(new Timestamp(calendar.getTime()));
            requestBook(rm, holder, dialog);
            builderDate.dismiss();
            dialog.dismiss();
        });

        refuseButton.setOnClickListener(click -> {
            Toast.makeText(context, "Richiesta non effettuata", Toast.LENGTH_LONG).show();
            builderDate.dismiss();
            dialog.dismiss();
        });

        builderDate.show();

    }

    /**
     * controlla se non esista già una richiesta in corso per lo stesso libro*/
    private boolean checkRequests(QueryDocumentSnapshot doc, RequestModel rm) {
        Log.d("CONFRONTO", doc.get("requestdBook") + " " + rm.getRequestedBook());
//        return (doc.get("status").equals("accepted") || doc.get("status").equals("ongoing"))
//                &&doc.get("receiver").equals(rm.getReceiver())
//                && doc.get("requestedBook").equals(rm.getRequestedBook())
//                && doc.get("sender").equals(rm.getSender())
//                && doc.get("thumbnail").equals(rm.getThumbnail())
//                && doc.get("title").equals(rm.getTitle())
//                && doc.get("type").equals(rm.getType());

        boolean exists = false;

        if (doc.get("status").equals("accepted") || doc.get("status").equals("ongoing")) {
            if (doc.get("receiver").equals(rm.getReceiver()) && doc.get("requestedBook").equals(rm.getRequestedBook())) {
                exists = true;
            }
        } else {
            if (doc.get("sender").equals(rm.getSender()) && doc.get("requestedBook").equals(rm.getRequestedBook())) {
                exists = true;
            }
        }
        return exists;
    }


    /**
     * effettua la richiesta del libro*/
    private void requestBook(RequestModel rm, ViewHolder holder, AlertDialog dialog) {
        dialog.dismiss();
        db.collection("requests").get().addOnCompleteListener(task -> {
            boolean err = false;
            if (task.isSuccessful()) {

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    //controlla se esiste già una richiesta uguale, non posso usare serialize di request model perchè ho lo status che varia
                    if (checkRequests(doc, rm)) {
                        err = true;
                    }
                }
                //se esiste già una richiesta da errore
                if (err) {
                    Toast.makeText(context, "Attenzione! La richiesta per " + results.get(holder.getAdapterPosition()).getBook().getTitle() + " esiste già!", Toast.LENGTH_LONG).show();
                } else {
                    db.collection("requests").add(rm.serialize()).addOnSuccessListener(documentReference -> {
                        Log.d("OKK", documentReference.getId());
                    }).addOnFailureListener(e -> Log.w("ERROR", "Error adding document", e));

                    Log.d("Sent to: ", results.get(holder.getAdapterPosition()).getUser().getNotificationToken());
                    try {
                        Notifications.sendPushNotification(Utils.CURRENT_USER
                                        .getFirstName() + " ti ha richiesto il libro: " + rm.getTitle(),
                                "Nuova richiesta",
                                results.get(holder.getAdapterPosition())
                                        .getUser()
                                        .getNotificationToken());
                    } catch (Exception e) {
                    }
                    Toast.makeText(context, "La richiesta è andata a buon fine!", Toast.LENGTH_LONG).show();
                }

            } else {
                Log.d("ERR", "Error getting documents: ", task.getException());
            }
        });
        //}

    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        // Layout
        private final ConstraintLayout book_selected;
        private final ImageView thumbnail;
        private final ImageView book_type;
        private final TextView title;
        private final TextView author;
        private final TextView book_owner;
        private final TextView neighborhood_owner;
        //private final TextView type;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Binding from xml layout to Class
            book_selected = itemView.findViewById(R.id.book);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            title = itemView.findViewById(R.id.book_title);
            author = itemView.findViewById(R.id.book_author);
            book_owner = itemView.findViewById(R.id.book_owner);
            neighborhood_owner = itemView.findViewById(R.id.neighborhood_owner);
            //type = itemView.findViewById(R.id.type);
            book_type = itemView.findViewById(R.id.icon_type);
        }
    }

}


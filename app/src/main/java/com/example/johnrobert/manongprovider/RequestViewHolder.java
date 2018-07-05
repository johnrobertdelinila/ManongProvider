package com.example.johnrobert.manongprovider;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class RequestViewHolder extends RecyclerView.ViewHolder{

    TextView serviceName, quotes, date, booked;

    public RequestViewHolder(@NonNull View itemView) {
        super(itemView);
        serviceName = itemView.findViewById(R.id.text_service_name);
//        quotes = itemView.findViewById(R.id.text_service_quotes);
//        date = itemView.findViewById(R.id.text_service_date);
//        booked = itemView.findViewById(R.id.text_booked_service);
    }


}

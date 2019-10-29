package com.anx.application.jcustomer.HistoryRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.anx.application.jcustomer.HistorySingleActivity;
import com.anx.application.jcustomer.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView rideId;
    public TextView time;
    public TextView rideCost;
    public TextView rideDestination;
    public HistoryViewHolder(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        rideId = (TextView) itemView.findViewById(R.id.rideId);
        time = (TextView) itemView.findViewById(R.id.time);
        rideCost = (TextView) itemView.findViewById(R.id.rideCost);
        rideDestination = (TextView) itemView.findViewById(R.id.destinationHistory);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        String rideIdTrim = rideId.getText().toString();
        b.putString("rideId", rideIdTrim.replace("Ride id: ", ""));
        intent.putExtras(b);
        v.getContext().startActivity(intent);
    }
}

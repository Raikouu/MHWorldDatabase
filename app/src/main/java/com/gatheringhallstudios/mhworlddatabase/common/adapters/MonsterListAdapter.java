package com.gatheringhallstudios.mhworlddatabase.common.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gatheringhallstudios.mhworlddatabase.R;
import com.gatheringhallstudios.mhworlddatabase.data.Monster;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to display a list of Monsters
 */

public class MonsterListAdapter extends RecyclerView.Adapter<MonsterListAdapter.ViewHolder> {

    List<Monster> monsterList = new ArrayList<>();

    public MonsterListAdapter(List<Monster> monsters) {
        monsterList = monsters;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater lf = LayoutInflater.from(parent.getContext());
        View view = lf.inflate(R.layout.listitem_monster, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Monster monster = monsterList.get(position);

        holder.monsterName.setText(monster.name);
        // TODO Set monster image
    }

    @Override
    public int getItemCount() {
        return monsterList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView monsterIcon;
        TextView monsterName;

        ViewHolder(View itemView) {
            super(itemView);
            monsterIcon = itemView.findViewById(R.id.icon_monster);
            monsterName = itemView.findViewById(R.id.name_monster);
        }
    }
}
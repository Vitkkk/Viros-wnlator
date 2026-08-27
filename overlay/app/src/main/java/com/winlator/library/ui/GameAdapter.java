package com.winlator.library.ui;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.R;
import com.winlator.library.data.GameEntity;
import com.winlator.win32.PEParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {
    public interface Listener {
        void onPlay(GameEntity game);
        void onLongPress(GameEntity game, View anchor);
    }

    private final List<GameEntity> games = new ArrayList<>();
    private final ExecutorService iconExecutor = Executors.newFixedThreadPool(2);
    private final Listener listener;

    public GameAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<GameEntity> items) {
        games.clear();
        if (items != null) games.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viros_game_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameEntity game = games.get(position);
        holder.title.setText(game.name);
        holder.subtitle.setText(engineLabel(game.detectedEngine));
        holder.cover.setImageResource(R.drawable.container_file_window);
        holder.cover.setTag(game.resolvedLocalPath);

        String path = game.resolvedLocalPath;
        if (path != null && !path.isEmpty()) {
            iconExecutor.execute(() -> {
                Bitmap bitmap = PEParser.extractIcon(new File(path));
                if (bitmap == null) return;
                holder.cover.post(() -> {
                    Object currentTag = holder.cover.getTag();
                    if (path.equals(currentTag)) holder.cover.setImageBitmap(bitmap);
                });
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onPlay(game));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(game, v);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    private static String engineLabel(String engine) {
        if (engine == null || engine.isEmpty() || "generic".equals(engine)) return "PC";
        switch (engine) {
            case "unity": return "Unity";
            case "unreal": return "Unreal Engine";
            case "godot": return "Godot";
            case "gamemaker": return "GameMaker";
            case "renpy": return "Ren'Py";
            case "rpgmaker": return "RPG Maker";
            case "electron": return "Electron";
            default: return engine;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final TextView title;
        final TextView subtitle;

        ViewHolder(View view) {
            super(view);
            cover = view.findViewById(R.id.VirosGameCover);
            title = view.findViewById(R.id.VirosGameTitle);
            subtitle = view.findViewById(R.id.VirosGameSubtitle);
        }
    }
}

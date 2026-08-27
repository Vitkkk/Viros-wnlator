package com.winlator.library.launch;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.XServerDisplayActivity;
import com.winlator.container.Container;
import com.winlator.library.data.GameDatabase;
import com.winlator.library.data.GameEntity;
import com.winlator.library.storage.LibraryPreferences;

import java.io.File;
import java.util.concurrent.Executors;

public class GameLaunchCoordinator {
    public interface Listener {
        void onPreparingSystem();
        void onLaunchError(String message);
    }

    private final AutomaticContainerProvider containerProvider = new AutomaticContainerProvider();

    public void launch(AppCompatActivity activity, GameEntity game, Listener listener) {
        if (game == null || game.resolvedLocalPath == null || game.resolvedLocalPath.isEmpty()) {
            listener.onLaunchError("O executável deste jogo não está disponível.");
            return;
        }

        File executable = new File(game.resolvedLocalPath);
        if (!executable.isFile()) {
            listener.onLaunchError("O arquivo do jogo não foi encontrado. Atualize a biblioteca ou selecione outro EXE.");
            return;
        }

        containerProvider.ensure(activity, new AutomaticContainerProvider.Callback() {
            @Override
            public void onReady(Container container) {
                LibraryPreferences preferences = new LibraryPreferences(activity);
                if (!ContainerDriveMapper.ensureVisible(container, preferences.getLocalRoot(), game.resolvedLocalPath)) {
                    listener.onLaunchError("A pasta deste jogo não pôde ser conectada ao ambiente Windows.");
                    return;
                }

                long now = System.currentTimeMillis();
                game.containerId = container.id;
                game.lastPlayed = now;
                Executors.newSingleThreadExecutor().execute(() -> GameDatabase.get(activity).gameDao().update(game));

                Intent intent = new Intent(activity, XServerDisplayActivity.class);
                intent.putExtra("container_id", container.id);
                intent.putExtra("exec_path", game.resolvedLocalPath);
                activity.startActivity(intent);
            }

            @Override
            public void onPreparingSystem() {
                listener.onPreparingSystem();
            }

            @Override
            public void onError(String message) {
                listener.onLaunchError(message);
            }
        });
    }
}

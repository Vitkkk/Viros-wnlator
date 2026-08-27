package com.winlator.library;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.library.data.GameDao;
import com.winlator.library.data.GameDatabase;
import com.winlator.library.data.GameEntity;
import com.winlator.library.launch.GameLaunchCoordinator;
import com.winlator.library.scan.GameScanner;
import com.winlator.library.storage.LibraryPreferences;
import com.winlator.library.storage.SafPathResolver;
import com.winlator.library.ui.GameAdapter;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.RootFSInstaller;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibraryActivity extends AppCompatActivity implements GameAdapter.Listener {
    private static final int PICK_GAMES_FOLDER_REQUEST = 9101;
    private static final int STORAGE_PERMISSION_REQUEST = 9102;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final GameScanner scanner = new GameScanner();
    private final GameLaunchCoordinator launchCoordinator = new GameLaunchCoordinator();

    private LibraryPreferences libraryPreferences;
    private GameAdapter adapter;
    private RecyclerView gameGrid;
    private LinearLayout setupPanel;
    private LinearLayout progressPanel;
    private TextView progressText;
    private TextView statusText;
    private GameEntity pendingLaunch;
    private boolean initialScanAttempted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppUtils.setActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viros_library_activity);

        libraryPreferences = new LibraryPreferences(this);
        gameGrid = findViewById(R.id.VirosGameGrid);
        setupPanel = findViewById(R.id.VirosSetupPanel);
        progressPanel = findViewById(R.id.VirosProgressPanel);
        progressText = findViewById(R.id.VirosProgressText);
        statusText = findViewById(R.id.VirosLibraryStatus);

        adapter = new GameAdapter(this);
        gameGrid.setAdapter(adapter);
        updateGridSpan(gameGrid.getWidth());
        gameGrid.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateGridSpan(right - left));

        Button selectFolder = findViewById(R.id.VirosSelectFolderButton);
        Button folderButton = findViewById(R.id.VirosFolderButton);
        Button refreshButton = findViewById(R.id.VirosRefreshButton);
        Button advancedButton = findViewById(R.id.VirosAdvancedButton);

        selectFolder.setOnClickListener(v -> openFolderPicker());
        folderButton.setOnClickListener(v -> openFolderPicker());
        refreshButton.setOnClickListener(v -> scanLibrary(false));
        advancedButton.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        boolean configured = libraryPreferences.getTreeUri() != null && libraryPreferences.getLocalRoot() != null;
        showSetup(!configured);
        if (configured) loadGames(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (libraryPreferences != null && libraryPreferences.getTreeUri() != null) loadGames(false);
    }

    private void updateGridSpan(int widthPx) {
        if (widthPx <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        int desiredWidthPx = Math.max(1, (int)(172f * density));
        int spanCount = Math.max(2, widthPx / desiredWidthPx);
        RecyclerView.LayoutManager current = gameGrid.getLayoutManager();
        if (!(current instanceof GridLayoutManager) || ((GridLayoutManager)current).getSpanCount() != spanCount) {
            gameGrid.setLayoutManager(new GridLayoutManager(this, spanCount));
        }
    }

    private void showSetup(boolean show) {
        setupPanel.setVisibility(show ? View.VISIBLE : View.GONE);
        gameGrid.setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.VirosRefreshButton).setEnabled(!show);
    }

    private void showProgress(String message) {
        progressText.setText(message);
        progressPanel.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressPanel.setVisibility(View.GONE);
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, PICK_GAMES_FOLDER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_GAMES_FOLDER_REQUEST || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;

        Uri treeUri = data.getData();
        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
        }
        catch (SecurityException e) {
            new AlertDialog.Builder(this)
                .setTitle("Não foi possível manter acesso à pasta")
                .setMessage("Escolha novamente a pasta e permita o acesso solicitado pelo Android.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        String localRoot = SafPathResolver.resolveLocalTreePath(this, treeUri);
        if (localRoot == null) {
            new AlertDialog.Builder(this)
                .setTitle("Escolha uma pasta local")
                .setMessage("Nesta versão, o jogo precisa estar no armazenamento local do aparelho. Pastas de provedores em nuvem não podem ser executadas diretamente pelo Wine.")
                .setPositiveButton("Escolher outra", (dialog, which) -> openFolderPicker())
                .setNegativeButton("Cancelar", null)
                .show();
            return;
        }

        libraryPreferences.setLibrary(treeUri, localRoot);
        initialScanAttempted = true;
        showSetup(false);
        ioExecutor.execute(() -> {
            GameDatabase.get(this).gameDao().clear();
            runOnUiThread(() -> scanLibrary(true));
        });
    }

    private void scanLibrary(boolean folderJustChanged) {
        Uri treeUri = libraryPreferences.getTreeUri();
        String localRoot = libraryPreferences.getLocalRoot();
        if (treeUri == null || localRoot == null) {
            showSetup(true);
            return;
        }

        showProgress("Encontrando jogos…");
        statusText.setText("Atualizando biblioteca");
        ioExecutor.execute(() -> {
            GameScanner.Result result = scanner.scan(this, treeUri, localRoot);
            for (GameScanner.Candidate candidate : result.automatic) saveCandidate(candidate);

            runOnUiThread(() -> {
                if (!result.ambiguous.isEmpty()) {
                    hideProgress();
                    resolveAmbiguous(result.ambiguous, 0);
                }
                else {
                    hideProgress();
                    loadGames(false);
                }
            });
        });
    }

    private void resolveAmbiguous(List<GameScanner.AmbiguousGroup> groups, int index) {
        if (index >= groups.size()) {
            loadGames(false);
            return;
        }

        GameScanner.AmbiguousGroup group = groups.get(index);
        CharSequence[] labels = new CharSequence[group.candidates.size()];
        for (int i = 0; i < group.candidates.size(); i++) {
            GameScanner.Candidate candidate = group.candidates.get(i);
            labels[i] = candidate.relativePath + "  •  " + formatSize(candidate.size);
        }

        final int[] selected = {0};
        new AlertDialog.Builder(this)
            .setTitle("Qual executável inicia " + (group.folderName.isEmpty() ? "este jogo" : group.folderName) + "?")
            .setSingleChoiceItems(labels, 0, (dialog, which) -> selected[0] = which)
            .setPositiveButton("Usar", (dialog, which) -> {
                GameScanner.Candidate candidate = group.candidates.get(selected[0]);
                showProgress("Adicionando " + candidate.displayName + "…");
                ioExecutor.execute(() -> {
                    saveCandidate(candidate);
                    runOnUiThread(() -> {
                        hideProgress();
                        resolveAmbiguous(groups, index + 1);
                    });
                });
            })
            .setNegativeButton("Ignorar", (dialog, which) -> resolveAmbiguous(groups, index + 1))
            .setCancelable(false)
            .show();
    }

    private void saveCandidate(GameScanner.Candidate candidate) {
        GameDao dao = GameDatabase.get(this).gameDao();
        GameEntity existing = dao.findByExecutableUri(candidate.executableUri);
        if (existing == null) {
            GameEntity game = candidate.toEntity();
            long id = dao.insert(game);
            if (id > 0) game.id = id;
        }
        else {
            existing.folderUri = candidate.gameFolderUri;
            existing.executableRelativePath = candidate.relativePath;
            existing.resolvedLocalPath = candidate.resolvedLocalPath;
            existing.detectedEngine = candidate.detectedEngine;
            existing.sourceLastModified = candidate.lastModified;
            existing.sourceSize = candidate.size;
            dao.update(existing);
        }
    }

    private void loadGames(boolean scanIfEmpty) {
        ioExecutor.execute(() -> {
            List<GameEntity> games = GameDatabase.get(this).gameDao().getAll();
            runOnUiThread(() -> {
                adapter.submit(games);
                statusText.setText(games.size() == 1 ? "1 jogo" : games.size() + " jogos");
                if (scanIfEmpty && games.isEmpty() && !initialScanAttempted) {
                    initialScanAttempted = true;
                    scanLibrary(false);
                }
            });
        });
    }

    @Override
    public void onPlay(GameEntity game) {
        if (requiresLegacyStoragePermission()) {
            pendingLaunch = game;
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST
            );
            return;
        }
        launchGame(game);
    }

    private boolean requiresLegacyStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return false;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != STORAGE_PERMISSION_REQUEST) return;
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) granted &= result == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingLaunch != null) {
            GameEntity game = pendingLaunch;
            pendingLaunch = null;
            launchGame(game);
        }
        else {
            pendingLaunch = null;
            showLaunchError("O Winlator precisa acessar os arquivos locais do jogo para executá-los.");
        }
    }

    private void launchGame(GameEntity game) {
        showProgress("Preparando " + game.name + "…");
        launchCoordinator.launch(this, game, new GameLaunchCoordinator.Listener() {
            @Override
            public void onPreparingSystem() {
                waitForSystemThenLaunch(game, 0);
            }

            @Override
            public void onLaunchError(String message) {
                hideProgress();
                showLaunchError(message);
            }
        });
    }

    private void waitForSystemThenLaunch(GameEntity game, int attempt) {
        if (isFinishing()) return;
        RootFS rootFS = RootFS.find(this);
        if (rootFS.isValid() && rootFS.getVersion() >= RootFSInstaller.LATEST_VERSION) {
            launchCoordinator.launch(this, game, new GameLaunchCoordinator.Listener() {
                @Override public void onPreparingSystem() {}
                @Override public void onLaunchError(String message) {
                    hideProgress();
                    showLaunchError(message);
                }
            });
            return;
        }

        if (attempt >= 240) {
            hideProgress();
            showLaunchError("A preparação do ambiente demorou mais que o esperado. Tente novamente.");
            return;
        }
        progressPanel.postDelayed(() -> waitForSystemThenLaunch(game, attempt + 1), 500L);
    }

    private void showLaunchError(String message) {
        new AlertDialog.Builder(this)
            .setTitle("O jogo não conseguiu iniciar")
            .setMessage(message)
            .setPositiveButton("Tentar novamente", (dialog, which) -> {
                if (pendingLaunch != null) launchGame(pendingLaunch);
            })
            .setNegativeButton("Fechar", null)
            .show();
    }

    @Override
    public void onLongPress(GameEntity game, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Jogar");
        menu.getMenu().add(0, 2, 1, game.favorite ? "Remover dos favoritos" : "Favoritar");
        menu.getMenu().add(0, 3, 2, "Editar controles");
        menu.getMenu().add(0, 4, 3, "Configuração avançada");
        menu.getMenu().add(0, 5, 4, "Remover da biblioteca");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    onPlay(game);
                    return true;
                case 2:
                    game.favorite = !game.favorite;
                    ioExecutor.execute(() -> {
                        GameDatabase.get(this).gameDao().update(game);
                        runOnUiThread(() -> loadGames(false));
                    });
                    return true;
                case 3: {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("edit_input_controls", true);
                    intent.putExtra("selected_profile_id", game.inputProfileId != null ? game.inputProfileId : 0);
                    startActivity(intent);
                    return true;
                }
                case 4:
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                case 5:
                    confirmRemove(game);
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void confirmRemove(GameEntity game) {
        new AlertDialog.Builder(this)
            .setTitle("Remover da biblioteca?")
            .setMessage("Os arquivos do jogo não serão apagados.")
            .setPositiveButton("Remover", (dialog, which) -> ioExecutor.execute(() -> {
                GameDatabase.get(this).gameDao().removeFromLibrary(game.id);
                runOnUiThread(() -> loadGames(false));
            }))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "tamanho desconhecido";
        double mb = bytes / (1024d * 1024d);
        if (mb < 1d) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024d);
        return String.format(Locale.ROOT, "%.1f MB", mb);
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        super.onDestroy();
    }
}

package com.secretary.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.secretary.R;
import com.secretary.room.AppDatabase;
import com.secretary.room.FileDao;
import com.secretary.room.FileEntity;
import com.secretary.room.FolderDao;
import com.secretary.room.FolderEntity;
import com.secretary.util.FileExportUtil;
import com.secretary.util.FileImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * File list activity scoped to a single category type or "all".
 * Shows folders and files with import, new-folder, export, move, delete support.
 */
public class FileListActivity extends BaseLockActivity {

    private static final int REQUEST_CODE_PICK_FILE = 1001;
    private static final int MAX_IMPORT_COUNT = 10;
    private static final int REQUEST_CODE_STORAGE_PERM = 1002;

    private String categoryType;
    private String categoryTitle;
    private long currentFolderId = 0; // 0 = root level
    private FolderEntity currentFolder;

    private FileDao fileDao;
    private FolderDao folderDao;
    private CompositeDisposable disposables = new CompositeDisposable();

    // Views
    private TextView tvTitle, tvPath, tvEmpty, tvFolderHeader, tvFilesHeader, tvSelectionCount;
    private View btnBack, selectionBar, btnSelectionCancel;
    private ImageButton btnImport, btnNewFolder, btnDelete, btnExport, btnMove;
    private RecyclerView recyclerFolders, recyclerFiles;

    // Adapters
    private FolderAdapter folderAdapter;
    private FileAdapter fileAdapter;

    // Selection
    private boolean isSelectionMode = false;
    private Set<Long> selectedFileIds = new HashSet<>();
    private Set<Long> selectedFolderIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        categoryType = getIntent().getStringExtra("category_type");
        if (categoryType == null) categoryType = "all";

        fileDao = AppDatabase.getInstance(this).fileDao();
        folderDao = AppDatabase.getInstance(this).folderDao();

        // Set title based on category
        switch (categoryType) {
            case "image": categoryTitle = getString(R.string.filter_images); break;
            case "video": categoryTitle = getString(R.string.filter_videos); break;
            case "audio": categoryTitle = getString(R.string.filter_audios); break;
            case "other": categoryTitle = getString(R.string.filter_others); break;
            default: categoryTitle = getString(R.string.filter_all); break;
        }

        initViews();
        setupClickListeners();
        loadFolders();
        loadFiles();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvPath = findViewById(R.id.tv_path);
        tvEmpty = findViewById(R.id.tv_empty);
        tvFolderHeader = findViewById(R.id.tv_folder_header);
        tvFilesHeader = findViewById(R.id.tv_files_header);
        btnBack = findViewById(R.id.btn_back);
        btnImport = findViewById(R.id.btn_import);
        btnNewFolder = findViewById(R.id.btn_new_folder);
        btnDelete = findViewById(R.id.btn_delete);
        btnExport = findViewById(R.id.btn_export);
        btnMove = findViewById(R.id.btn_move);
        selectionBar = findViewById(R.id.selection_bar);
        tvSelectionCount = findViewById(R.id.tv_selection_count);
        btnSelectionCancel = findViewById(R.id.btn_selection_cancel);

        tvTitle.setText(categoryTitle);

        recyclerFolders = findViewById(R.id.recycler_folders);
        recyclerFiles = findViewById(R.id.recycler_files);

        int spanCount = getResources().getConfiguration().screenWidthDp > 600 ? 4 : 3;
        recyclerFiles.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerFolders.setLayoutManager(new GridLayoutManager(this, spanCount));

        fileAdapter = new FileAdapter();
        recyclerFiles.setAdapter(fileAdapter);

        folderAdapter = new FolderAdapter();
        recyclerFolders.setAdapter(folderAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (isSelectionMode) {
                exitSelectionMode();
                return;
            }
            if (currentFolderId != 0) {
                if (currentFolder != null && currentFolder.getParentId() != 0) {
                    openFolder(currentFolder.getParentId());
                } else {
                    openFolder(0);
                }
            } else {
                finish();
            }
        });

        btnImport.setOnClickListener(v -> {
            if (isSelectionMode) exitSelectionMode();
            openFilePicker();
        });

        btnNewFolder.setOnClickListener(v -> showNewFolderDialog());

        btnDelete.setOnClickListener(v -> deleteSelectedItems());

        btnExport.setOnClickListener(v -> exportSelectedFiles());

        btnMove.setOnClickListener(v -> showMoveFolderPicker());

        btnSelectionCancel.setOnClickListener(v -> exitSelectionMode());
    }

    // ========== File Picker ==========

    private void openFilePicker() {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != 0) {
            requestPermissions(
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_STORAGE_PERM);
            return;
        }
        openFilePickerInternal();
    }

    private void openFilePickerInternal() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            Uri singleUri = data.getData();
            if (singleUri != null) {
                uris.add(singleUri);
            }
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null) uris.add(uri);
                }
            }
            importFiles(uris);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERM) {
            for (int result : grantResults) {
                if (result != 0) {
                    Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            openFilePickerInternal();
        }
    }

    private void importFiles(List<Uri> uris) {
        if (uris.isEmpty()) return;

        if (uris.size() > MAX_IMPORT_COUNT) {
            Toast.makeText(this, "最多只能选择 " + MAX_IMPORT_COUNT + " 个文件，已自动取前 " + MAX_IMPORT_COUNT + " 个", Toast.LENGTH_LONG).show();
            uris = uris.subList(0, MAX_IMPORT_COUNT);
        }

        Toast.makeText(this, "正在导入 " + uris.size() + " 个文件...", Toast.LENGTH_SHORT).show();

        final int[] completed = {0};
        final int[] success = {0};
        final int[] failed = {0};
        final int total = uris.size();

        for (Uri uri : uris) {
            disposables.add(
                    FileImportUtil.importFile(this, uri)
                            .flatMap(info -> {
                                FileEntity entity = new FileEntity(
                                        info.name, info.path, info.type,
                                        info.mimeType, info.size
                                );
                                entity.setFolderId(currentFolderId);
                                return fileDao.insert(entity);
                            })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    id -> {
                                        completed[0]++;
                                        success[0]++;
                                        if (completed[0] == total) {
                                            String msg = "导入完成：" + success[0] + " 个成功";
                                            if (failed[0] > 0) msg += "，" + failed[0] + " 个失败";
                                            Toast.makeText(FileListActivity.this, msg, Toast.LENGTH_LONG).show();
                                            loadFiles();
                                        }
                                    },
                                    error -> {
                                        completed[0]++;
                                        failed[0]++;
                                        if (completed[0] == total) {
                                            String msg = "导入完成：" + success[0] + " 个成功";
                                            if (failed[0] > 0) msg += "，" + failed[0] + " 个失败";
                                            Toast.makeText(FileListActivity.this, msg, Toast.LENGTH_LONG).show();
                                            loadFiles();
                                        }
                                    }
                            )
                );
        }
    }

    // ========== Folder Navigation ==========

    private void openFolder(long folderId) {
        currentFolderId = folderId;
        currentFolder = null;

        if (folderId == 0) {
            tvPath.setVisibility(View.GONE);
            btnNewFolder.setVisibility(View.VISIBLE);
            tvTitle.setText(categoryTitle);
        } else {
            btnNewFolder.setVisibility(View.VISIBLE);
            disposables.add(folderDao.getFolderById(folderId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(folder -> {
                        currentFolder = folder;
                        tvPath.setVisibility(View.VISIBLE);
                        tvPath.setText("> " + folder.getName());
                        tvTitle.setText(folder.getName());
                    }, Throwable::printStackTrace));
        }

        exitSelectionMode();
        loadFolders();
        loadFiles();
    }

    // ========== Data Loading ==========

    private void loadFolders() {
        Flowable<List<FolderEntity>> flowable;

        if (currentFolderId == 0) {
            if ("all".equals(categoryType)) {
                flowable = folderDao.getSubFolders(0);
            } else {
                flowable = folderDao.getRootFoldersByType(categoryType);
            }
        } else {
            flowable = folderDao.getSubFolders(currentFolderId);
        }

        disposables.add(flowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(folders -> {
                    folderAdapter.setFolders(folders);
                    boolean hasFolders = !folders.isEmpty();
                    recyclerFolders.setVisibility(hasFolders ? View.VISIBLE : View.GONE);
                    tvFolderHeader.setVisibility(hasFolders ? View.VISIBLE : View.GONE);
                }, Throwable::printStackTrace));
    }

    private void loadFiles() {
        Flowable<List<FileEntity>> flowable;

        if (currentFolderId == 0) {
            if ("all".equals(categoryType)) {
                flowable = fileDao.getAllFiles();
            } else {
                flowable = fileDao.getFilesByTypeRoot(categoryType);
            }
        } else {
            if ("all".equals(categoryType)) {
                flowable = fileDao.getFilesByFolder(currentFolderId);
            } else {
                flowable = fileDao.getFilesByTypeAndFolder(categoryType, currentFolderId);
            }
        }

        disposables.add(flowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(files -> {
                    fileAdapter.setFiles(files);
                    tvEmpty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
                }, Throwable::printStackTrace));
    }

    // ========== Folder Creation ==========

    private void showNewFolderDialog() {
        final EditText input = new EditText(this);
        input.setHint("文件夹名称");

        new AlertDialog.Builder(this)
                .setTitle("新建文件夹")
                .setView(input)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createFolder(name);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void createFolder(String name) {
        String folderType = categoryType;
        if ("all".equals(folderType)) {
            if (currentFolder != null) {
                folderType = currentFolder.getType();
            } else {
                folderType = "other";
            }
        }

        FolderEntity folder = new FolderEntity(name, folderType, currentFolderId);
        disposables.add(folderDao.insert(folder)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> {
                    Toast.makeText(this, "文件夹创建成功", Toast.LENGTH_SHORT).show();
                }, Throwable::printStackTrace));
    }

    // ========== Selection Mode ==========

    private void enterSelectionMode() {
        isSelectionMode = true;
        selectedFileIds.clear();
        selectedFolderIds.clear();
        showSelectionUI();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedFileIds.clear();
        selectedFolderIds.clear();
        hideSelectionUI();
    }

    private void showSelectionUI() {
        selectionBar.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
        btnExport.setVisibility(View.VISIBLE);
        btnMove.setVisibility(View.VISIBLE);
        btnNewFolder.setVisibility(View.GONE);
        btnImport.setVisibility(View.GONE);
        recyclerFiles.setPadding(8, 8, 8, 8);
        updateSelectionCount();
        fileAdapter.notifyDataSetChanged();
        folderAdapter.notifyDataSetChanged();
    }

    private void hideSelectionUI() {
        selectionBar.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);
        btnExport.setVisibility(View.GONE);
        btnMove.setVisibility(View.GONE);
        btnNewFolder.setVisibility(View.VISIBLE);
        btnImport.setVisibility(View.VISIBLE);
        fileAdapter.notifyDataSetChanged();
        folderAdapter.notifyDataSetChanged();
    }

    private void updateSelectionCount() {
        int total = selectedFileIds.size() + selectedFolderIds.size();
        tvSelectionCount.setText("已选择 " + total + " 项");
    }

    // ========== Export Files to System ==========

    private void exportSelectedFiles() {
        if (selectedFileIds.isEmpty()) {
            Toast.makeText(this, "请先选择要导出的文件", Toast.LENGTH_SHORT).show();
            return;
        }

        int fileCount = selectedFileIds.size();
        new AlertDialog.Builder(this)
                .setTitle("导出文件")
                .setMessage("将 " + fileCount + " 个文件导出到系统「下载」文件夹？")
                .setPositiveButton("导出", (dialog, which) -> {
                    doExportSelected();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void doExportSelected() {
        Toast.makeText(this, "正在导出 " + selectedFileIds.size() + " 个文件...", Toast.LENGTH_SHORT).show();

        final int[] completed = {0};
        final int[] total = {selectedFileIds.size()};

        for (long fileId : selectedFileIds) {
            disposables.add(fileDao.getFileById(fileId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(entity -> {
                        if (entity != null) {
                            disposables.add(FileExportUtil.exportFile(
                                    FileListActivity.this,
                                    entity.getPath(),
                                    entity.getName()
                            )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    result -> {
                                        completed[0]++;
                                        if (completed[0] == total[0]) {
                                            Toast.makeText(FileListActivity.this,
                                                    "导出完成：" + total[0] + " 个文件已保存到「下载」文件夹",
                                                    Toast.LENGTH_LONG).show();
                                            exitSelectionMode();
                                        }
                                    },
                                    error -> {
                                        completed[0]++;
                                        Toast.makeText(FileListActivity.this,
                                                "导出失败：" + entity.getName() + " - " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        if (completed[0] == total[0]) {
                                            exitSelectionMode();
                                        }
                                    }
                            ));
                        } else {
                            completed[0]++;
                            if (completed[0] == total[0]) {
                                exitSelectionMode();
                            }
                        }
                    }, Throwable::printStackTrace));
        }
    }

    // ========== Move Files to Another Folder ==========

    /**
     * Show a dialog to pick a destination folder for moving selected files.
     * Lists all folders in the current category at root level + sub-folders.
     */
    private void showMoveFolderPicker() {
        if (selectedFileIds.isEmpty()) {
            Toast.makeText(this, "请先选择要移动的文件", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load all available folders that are NOT the current folder
        Flowable<List<FolderEntity>> flowable;
        if ("all".equals(categoryType)) {
            // Load all root-level folders
            flowable = folderDao.getSubFolders(0);
        } else {
            flowable = folderDao.getRootFoldersByType(categoryType);
        }

        disposables.add(flowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(folders -> {
                    showFolderPickerDialog(folders);
                }, error -> {
                    Toast.makeText(this, "加载文件夹列表失败", Toast.LENGTH_SHORT).show();
                }));
    }

    private void showFolderPickerDialog(List<FolderEntity> folders) {
        // Build list: "根目录 (不移动)" + all other folders
        final List<FolderOption> options = new ArrayList<>();
        boolean currentIsRoot = (currentFolderId == 0);

        // Add "root" option
        if (!currentIsRoot) {
            options.add(new FolderOption(0, "根目录（根目录）", false));
        } else {
            options.add(new FolderOption(0, "根目录（不移动）", true));
        }

        for (FolderEntity folder : folders) {
            if (folder.getId() == currentFolderId) continue; // skip current folder
            options.add(new FolderOption(folder.getId(), folder.getName(), false));
        }

        String[] items = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            items[i] = options.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("移动到...")
                .setItems(items, (dialog, which) -> {
                    FolderOption selected = options.get(which);
                    if (selected.isCurrent) {
                        Toast.makeText(this, "文件已经在当前目录", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    doMoveSelected(selected.folderId);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private static class FolderOption {
        long folderId;
        String name;
        boolean isCurrent;
        FolderOption(long folderId, String name, boolean isCurrent) {
            this.folderId = folderId;
            this.name = name;
            this.isCurrent = isCurrent;
        }
    }

    private void doMoveSelected(long targetFolderId) {
        Toast.makeText(this, "正在移动文件...", Toast.LENGTH_SHORT).show();

        final int[] completed = {0};
        final int total = selectedFileIds.size();

        for (long fileId : selectedFileIds) {
            disposables.add(fileDao.getFileById(fileId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(entity -> {
                        if (entity != null) {
                            entity.setFolderId(targetFolderId);
                            disposables.add(fileDao.update(entity)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            count -> {
                                                completed[0]++;
                                                if (completed[0] == total) {
                                                    Toast.makeText(FileListActivity.this,
                                                            total + " 个文件已移动", Toast.LENGTH_SHORT).show();
                                                    exitSelectionMode();
                                                }
                                            },
                                            error -> {
                                                completed[0]++;
                                                Toast.makeText(FileListActivity.this,
                                                        "移动失败：" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                if (completed[0] == total) exitSelectionMode();
                                            }
                                    ));
                        } else {
                            completed[0]++;
                            if (completed[0] == total) exitSelectionMode();
                        }
                    }, Throwable::printStackTrace));
        }
    }

    // ========== Delete ==========

    private void deleteSelectedItems() {
        if (selectedFileIds.isEmpty() && selectedFolderIds.isEmpty()) {
            if (isSelectionMode) exitSelectionMode();
            else enterSelectionMode();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    // Delete selected files
                    for (long id : selectedFileIds) {
                        disposables.add(fileDao.getFileById(id)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(entity -> {
                                    if (entity != null) {
                                        File file = new File(entity.getPath());
                                        if (file.exists()) file.delete();
                                        disposables.add(fileDao.deleteById(id)
                                                .subscribeOn(Schedulers.io())
                                                .subscribe());
                                    }
                                }, Throwable::printStackTrace));
                    }

                    // Delete selected folders (files inside move to root)
                    for (long folderId : selectedFolderIds) {
                        disposables.add(fileDao.moveFilesToRoot(folderId)
                                .subscribeOn(Schedulers.io())
                                .subscribe());
                        disposables.add(folderDao.deleteById(folderId)
                                .subscribeOn(Schedulers.io())
                                .subscribe());
                    }

                    Toast.makeText(FileListActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    exitSelectionMode();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    // ========== FolderAdapter ==========

    class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {
        private List<FolderEntity> folders = new ArrayList<>();

        void setFolders(List<FolderEntity> folders) {
            this.folders = folders;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FolderEntity folder = folders.get(position);
            holder.tvName.setText(folder.getName());

            if (!isSelectionMode) {
                holder.itemView.setOnClickListener(v -> openFolder(folder.getId()));
                holder.itemView.setOnLongClickListener(v -> {
                    if (!isSelectionMode) {
                        enterSelectionMode();
                    }
                    long id = folder.getId();
                    if (selectedFolderIds.contains(id)) {
                        selectedFolderIds.remove(id);
                    } else {
                        selectedFolderIds.add(id);
                    }
                    updateSelectionCount();
                    notifyDataSetChanged();
                    return true;
                });
            } else {
                holder.itemView.setOnClickListener(v -> {
                    long id = folder.getId();
                    if (selectedFolderIds.contains(id)) {
                        selectedFolderIds.remove(id);
                    } else {
                        selectedFolderIds.add(id);
                    }
                    updateSelectionCount();
                    notifyDataSetChanged();
                });
                holder.itemView.setOnLongClickListener(null);
            }

            // Highlight selected
            holder.itemView.setAlpha(selectedFolderIds.contains(folder.getId()) ? 0.5f : 1f);
        }

        @Override
        public int getItemCount() { return folders.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_folder_name);
            }
        }
    }

    // ========== FileAdapter ==========

    class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
        private List<FileEntity> files = new ArrayList<>();

        void setFiles(List<FileEntity> files) { this.files = files; notifyDataSetChanged(); }

        FileEntity getFileById(long id) {
            for (FileEntity f : files) if (f.getId() == id) return f;
            return null;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FileEntity file = files.get(position);
            holder.tvFileName.setText(file.getName());
            holder.tvFileSize.setText(formatFileSize(file.getSize()));

            if (isSelectionMode) {
                // Selection mode: show checkbox
                holder.cbSelect.setVisibility(View.VISIBLE);
                // Clear stale long-click listener from previous normal-mode binding
                holder.itemView.setOnLongClickListener(null);
                holder.cbSelect.setChecked(selectedFileIds.contains(file.getId()));

                holder.itemView.setOnClickListener(v -> {
                    long id = file.getId();
                    if (selectedFileIds.contains(id)) {
                        selectedFileIds.remove(id);
                        holder.cbSelect.setChecked(false);
                    } else {
                        selectedFileIds.add(id);
                        holder.cbSelect.setChecked(true);
                    }
                    updateSelectionCount();
                });
            } else {
                holder.cbSelect.setVisibility(View.GONE);

                // Normal mode: open file on click
                String type = file.getType() != null ? file.getType() : "other";
                if ("image".equals(type)) {
                    holder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(FileListActivity.this, ImagePreviewActivity.class);
                        intent.putExtra("file_path", file.getPath());
                        startActivity(intent);
                    });
                } else if ("video".equals(type)) {
                    holder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(FileListActivity.this, VideoPlayerActivity.class);
                        intent.putExtra("file_path", file.getPath());
                        startActivity(intent);
                    });
                } else {
                    holder.itemView.setOnClickListener(null);
                }

                holder.itemView.setOnLongClickListener(v -> {
                    if (!isSelectionMode) {
                        enterSelectionMode();
                    }
                    long id = file.getId();
                    selectedFileIds.add(id);
                    holder.cbSelect.setChecked(true);
                    updateSelectionCount();
                    notifyDataSetChanged();
                    return true;
                });
            }

            // --- Thumbnail ---
            String type = file.getType() != null ? file.getType() : "other";

            if ("image".equals(type)) {
                holder.ivPlayOverlay.setVisibility(View.GONE);
                holder.tvDuration.setVisibility(View.GONE);
                Glide.with(FileListActivity.this)
                        .load(new File(file.getPath()))
                        .apply(new RequestOptions().override(300, 300).centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.NONE))
                        .into(holder.ivThumbnail);

            } else if ("video".equals(type)) {
                holder.ivPlayOverlay.setVisibility(View.VISIBLE);
                holder.tvDuration.setVisibility(View.VISIBLE);
                Glide.with(FileListActivity.this)
                        .load(new File(file.getPath()))
                        .apply(new RequestOptions().override(300, 300).centerCrop()
                                .frame(1000000).diskCacheStrategy(DiskCacheStrategy.NONE))
                        .into(holder.ivThumbnail);
            } else {
                holder.ivPlayOverlay.setVisibility(View.GONE);
                holder.tvDuration.setVisibility(View.GONE);
                if ("audio".equals(type)) {
                    holder.ivThumbnail.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_sort_by_size);
                }
                holder.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        }

        @Override
        public int getItemCount() { return files.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail, ivPlayOverlay;
            TextView tvFileName, tvFileSize, tvDuration;
            android.widget.CheckBox cbSelect;

            ViewHolder(View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
                ivPlayOverlay = itemView.findViewById(R.id.iv_play_overlay);
                tvFileName = itemView.findViewById(R.id.tv_file_name);
                tvFileSize = itemView.findViewById(R.id.tv_file_size);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                cbSelect = itemView.findViewById(R.id.cb_select);
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        else if (bytes < 1024 * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
        else return String.format(Locale.US, "%.1f GB", bytes / (1024f * 1024f * 1024f));
    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }
}

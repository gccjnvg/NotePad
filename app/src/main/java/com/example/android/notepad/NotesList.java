/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

/**
 * 显示笔记列表。如果启动此 Activity 的 Intent 中提供了 {@link Uri}，
 * 则显示来自该 URI 的笔记；否则默认显示 {@link NotePadProvider} 中的内容。
 */
public class NotesList extends ListActivity {

    private static final String SEARCH_QUERY_KEY = "searchQuery";
    private SearchView searchView;
    private String currentSearchQuery = "";

    // 在 NotesList 类中添加背景颜色常量
    private static final int MENU_BACKGROUND_COLOR = Menu.FIRST + 100;
    private static final String PREF_BACKGROUND_COLOR = "background_color";
    private static final int DEFAULT_BACKGROUND_COLOR = 0xFFFFFFFF;

    // 用于日志记录
    private static final String TAG = "NotesList";

    /**
     * 游标适配器所需的列
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2 时间戳列
            NotePad.Notes.COLUMN_NAME_COLOR // 3 颜色列
    };
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2; // 时间戳列索引
    private static final int COLUMN_INDEX_COLOR = 3; // 颜色列索引

    /** 标题列的索引 */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * 当 Android 从头开始启动此 Activity 时调用 onCreate。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置布局，包含搜索框
        setContentView(R.layout.noteslist_with_search);

        // 应用保存的背景颜色
        SharedPreferences prefs = getSharedPreferences("notepad_prefs", MODE_PRIVATE);
        int backgroundColor = prefs.getInt(PREF_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);
        applyBackgroundColor(backgroundColor);

        // 恢复搜索状态
        if (savedInstanceState != null) {
            currentSearchQuery = savedInstanceState.getString(SEARCH_QUERY_KEY, "");
        }

        // 用户不需要按住键即可使用菜单快捷方式。
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // 获取启动此 Activity 的 Intent。
        Intent intent = getIntent();

        // 如果 Intent 中没有关联数据，则设置数据为默认 URI，访问笔记列表。
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        // 为 ListView 设置上下文菜单激活的回调。
        getListView().setOnCreateContextMenuListener(this);

        // 初始化搜索视图
        initSearchView();

        // 执行查询
        performQuery();
    }

    // 添加保存状态的方法
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SEARCH_QUERY_KEY, currentSearchQuery);
    }

    // 添加初始化搜索视图的方法
    private void initSearchView() {
        searchView = (SearchView) findViewById(R.id.search_view);
        if (searchView != null) {
            searchView.setIconifiedByDefault(false);
            searchView.setQueryHint("搜索笔记...");
            searchView.setOnQueryTextListener(new OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentSearchQuery = query;
                    performQuery();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentSearchQuery = newText;
                    performQuery();
                    return true;
                }
            });

            // 设置初始查询文本
            if (!currentSearchQuery.isEmpty()) {
                searchView.setQuery(currentSearchQuery, false);
            }
        }
    }

    // 添加执行查询的方法
    private void performQuery() {
        String selection = null;
        String[] selectionArgs = null;

        // 如果有搜索查询，则添加筛选条件
        if (!currentSearchQuery.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + currentSearchQuery + "%"};
        }

        /* 执行托管查询。Activity 在需要时处理关闭和重新查询游标。
         */
        Cursor cursor = managedQuery(
                getIntent().getData(),            // 使用提供者的默认内容 URI。
                PROJECTION,                       // 返回每个笔记的 ID 和标题。
                selection,                        // 筛选条件
                selectionArgs,                    // 筛选参数
                NotePad.Notes.DEFAULT_SORT_ORDER  // 使用默认排序顺序。
        );

        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_COLOR
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.timestamp,
                R.id.note_color
        };

        // 创建 ListView 的后备适配器。
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,                             // ListView 的上下文
                R.layout.noteslist_item,          // 指向列表项的 XML
                cursor,                           // 获取项目的游标
                dataColumns,
                viewIDs
        );

        // 添加时间戳和颜色格式化器
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == COLUMN_INDEX_MODIFICATION_DATE) {
                    long timestamp = cursor.getLong(columnIndex);
                    // 格式化时间为 年-月-日 上午/下午 时:分
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd a hh:mm", Locale.getDefault());
                    String dateStr = sdf.format(new Date(timestamp));
                    ((TextView) view).setText(dateStr);
                    return true;
                } else if (columnIndex == COLUMN_INDEX_COLOR) {
                    int color = cursor.getInt(columnIndex);
                    if (color != -1) {
                        view.setBackgroundColor(color);
                    } else {
                        view.setBackgroundColor(Color.TRANSPARENT);
                    }
                    return true;
                }
                return false;
            }
        });

        // 将 ListView 的适配器设置为刚刚创建的游标适配器。
        setListAdapter(adapter);
    }

    /**
     * 当用户首次点击设备的菜单按钮时调用此方法。
     * Android 传递一个填充了项目的 Menu 对象。
     *
     * 设置一个提供插入选项和此 Activity 替代操作列表的菜单。
     * 其他想要处理笔记的应用可以通过提供包含类别 ALTERNATIVE 和
     * mimeTYpe NotePad.Notes.CONTENT_TYPE 的意图过滤器来"注册"自己。
     * 如果这样做，onCreateOptionsMenu() 中的代码会将包含意图过滤器的 Activity 添加到选项列表中。
     * 实际上，菜单会向用户提供可以处理笔记的其他应用。
     * @param menu Menu 对象，应向其中添加菜单项。
     * @return 总是返回 true。应显示菜单。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 从 XML 资源展开菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 添加背景颜色选项
        menu.add(0, MENU_BACKGROUND_COLOR, 0, "背景颜色")
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // 生成可在整个列表上执行的附加操作。
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // 如果剪贴板上有数据，则启用粘贴菜单项。
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // 如果剪贴板包含项目，则启用菜单的粘贴选项。
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // 如果剪贴板为空，则禁用菜单的粘贴选项。
            mPasteItem.setEnabled(false);
        }

        // 获取当前显示的笔记数量。
        final boolean haveItems = getListAdapter().getCount() > 0;

        // 如果列表中有任何笔记（这意味着其中一个被选中），
        // 那么我们需要生成可在当前选择上执行的操作。
        // 这将是我们的特定操作和任何扩展的组合。
        if (haveItems) {

            // 这是选中的项目。
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // 创建一个包含一个元素的 Intent 数组。这将用于发送基于选中菜单项的 Intent。
            Intent[] specifics = new Intent[1];

            // 将数组中的 Intent 设置为对选中笔记 URI 的 EDIT 操作。
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // 创建一个包含一个元素的菜单项数组。这将包含 EDIT 选项。
            MenuItem[] items = new MenuItem[1];

            // 使用选中笔记的 URI 创建一个没有特定操作的 Intent。
            Intent intent = new Intent(null, uri);

            /* 将类别 ALTERNATIVE 添加到 Intent 中，以笔记 ID URI 作为其数据。
             * 这将 Intent 准备为菜单中的替代选项分组位置。
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * 向菜单添加替代项
             */
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,  // 将 Intent 作为替代组中的选项添加。
                    Menu.NONE,                  // 不需要唯一的项目 ID。
                    Menu.NONE,                  // 替代项不需要按顺序排列。
                    null,                       // 排除调用者的名称。
                    specifics,                  // 这些特定选项必须首先出现。
                    intent,                     // 这些 Intent 对象映射到 specifics 中的选项。
                    Menu.NONE,                  // 不需要标志。
                    items                       // 从 specifics-to-Intents 映射生成的菜单项
            );
            // 如果存在编辑菜单项，则为其添加快捷方式。
            if (items[0] != null) {

                // 将编辑菜单项快捷方式设置为数字"1"，字母"e"
                items[0].setShortcut('1', 'e');
            }
        } else {
            // 如果列表为空，则从菜单中移除任何现有的替代操作
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        // 显示菜单
        return true;
    }

    /**
     * 当用户从菜单中选择选项但未选择列表中的项目时调用此方法。
     * 如果选项是 INSERT，则发送一个带有 ACTION_INSERT 操作的新 Intent。
     * 来自传入 Intent 的数据被放入新 Intent 中。
     * 实际上，这会触发 NotePad 应用中的 NoteEditor activity。
     *
     * 如果项目不是 INSERT，则很可能是来自其他应用的替代选项。
     * 调用父方法来处理该项目。
     * @param item 用户选择的菜单项
     * @return 如果选择了 INSERT 菜单项则返回 true；否则返回调用父方法的结果。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            /*
             * 使用 Intent 启动新 Activity。Activity 的意图过滤器必须有 ACTION_INSERT 操作。
             * 没有设置类别，因此假定为 DEFAULT。实际上，这会在 NotePad 中启动 NoteEditor Activity。
             */
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (item.getItemId() == R.id.menu_paste) {
            /*
             * 使用 Intent 启动新 Activity。Activity 的意图过滤器必须有 ACTION_PASTE 操作。
             * 没有设置类别，因此假定为 DEFAULT。实际上，这会在 NotePad 中启动 NoteEditor Activity。
             */
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (item.getItemId() == MENU_BACKGROUND_COLOR) {
            // 显示颜色选择对话框
            showBackgroundColorDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 添加显示背景颜色选择对话框的方法
    private void showBackgroundColorDialog() {
        // 获取当前背景颜色
        SharedPreferences prefs = getSharedPreferences("notepad_prefs", MODE_PRIVATE);
        int currentColor = prefs.getInt(PREF_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);

        // 创建颜色选择对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择背景颜色");

        // 创建颜色选项
        final int[] colors = {
                0xFFFFFFFF, // 白色
                0xFFE0E0E0, // 浅灰色
                0xFFBBDEFB, // 蓝色
                0xFFC8E6C9, // 绿色
                0xFFFFF9C4, // 黄色
                0xFFFFCCBC, // 橙色
                0xFFE1BEE7, // 紫色
                0xFFFFCDD2  // 红色
        };

        final String[] colorNames = {
                "白色", "浅灰色", "蓝色", "绿色", "黄色", "橙色", "紫色", "红色"
        };

        // 查找当前颜色的索引
        int selectedIndex = 0;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == currentColor) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(colorNames, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 保存选择的颜色
                SharedPreferences.Editor editor = getSharedPreferences("notepad_prefs", MODE_PRIVATE).edit();
                editor.putInt(PREF_BACKGROUND_COLOR, colors[which]);
                editor.apply();

                // 应用背景颜色
                applyBackgroundColor(colors[which]);

                // 关闭对话框
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 添加应用背景颜色的方法
    private void applyBackgroundColor(int color) {
        // 获取根视图并设置背景颜色
        View rootView = findViewById(android.R.id.list).getRootView();
        rootView.setBackgroundColor(color);

        // 如果有搜索视图，也设置其背景
        View searchContainer = findViewById(R.id.search_container);
        if (searchContainer != null) {
            searchContainer.setBackgroundColor(color);
        }
    }

    /**
     * 当用户在列表中的笔记上右键单击时调用此方法。
     * NotesList 将自身注册为其 ListView 的上下文菜单处理程序（在 onCreate() 中完成）。
     *
     * 只有 COPY 和 DELETE 选项可用。
     *
     * 右键单击等同于长按。
     *
     * @param menu 应向其中添加项目的 ContexMenu 对象。
     * @param view 正在构建上下文菜单的视图。
     * @param menuInfo 与视图关联的数据。
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // 菜单项的数据。
        AdapterView.AdapterContextMenuInfo info;

        // 尝试获取 ListView 中长按项目的位??。
        try {
            // 将传入的数据对象转换为 AdapterView 对象的类型。
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // 如果无法转换菜单对象，则记录错误。
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * 获取与所选位置项目关联的数据。getItem() 返回 ListView 背部适配器关联的所有数据。
         * 在 NotesList 中，适配器将笔记的所有数据与其列表项关联。
         * 因此，getItem() 将该数据作为游标返回。
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // 如果游标为空，则适配器无法从提供者获取数据，向调用者返回 null。
        if (cursor == null) {
            // 出于某种原因请求的项目不可用，不执行任何操作
            return;
        }

        // 从 XML 资源展开菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // 将菜单标题设置为选中笔记的标题。
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // 为任何其他可以处理它的活动追加到菜单项
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * 当用户从上下文菜单中选择项目时调用此方法
     * （参见 onCreateContextMenu()）。实际处理的唯一菜单项是 DELETE 和 COPY。
     * 其他任何项目都是替代选项，应进行默认处理。
     *
     * @param item 选中的菜单项
     * @return 如果菜单项是 DELETE 且不需要默认处理则返回 true，
     * 否则返回 false，触发该项的默认处理。
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // 菜单项的数据。
        AdapterView.AdapterContextMenuInfo info;

        /*
         * 从菜单项获取额外信息。当 Notes 列表中的笔记被长按时，
         * 会出现上下文菜单。菜单项的菜单会自动获取与长按笔记关联的数据。
         * 数据来自支持列表的提供者。
         *
         * 笔记数据通过 ContextMenuInfo 对象传递给上下文菜单创建例程。
         *
         * 当单击上下文菜单项之一时，相同的数据连同笔记 ID 一起通过 item 参数传递给 onContextItemSelected()。
         */
        try {
            // 将项目中的数据对象转换为 AdapterView 对象的类型。
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // 如果无法转换对象，则记录错误
            Log.e(TAG, "bad menuInfo", e);

            // 触发菜单项的默认处理。
            return false;
        }
        // 将选中笔记的 ID 追加到随传入 Intent 发送的 URI 中。
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * 获取菜单项的 ID 并将其与已知操作进行比较。
         */
        int id = item.getItemId();
        if (id == R.id.context_open) {
            // 启动 activity 以查看/编辑当前选中的项目
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) {
            // 获取剪贴板服务的句柄。
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // 将笔记 URI 复制到剪贴板。实际上，这是复制笔记本身
            clipboard.setPrimaryClip(ClipData.newUri(   // 包含 URI 的新剪贴板项目
                    getContentResolver(),               // 用于检索 URI 信息的解析器
                    "Note",                             // 剪贴的标签
                    noteUri));                          // URI

            // 返回调用者并跳过进一步处理。
            return true;
        } else if (id == R.id.context_delete) {
            // 通过传入笔记 ID 格式的 URI 从提供者中删除笔记。
            getContentResolver().delete(
                    noteUri,  // 提供者的 URI
                    null,     // 不需要 where 子句，因为只传递单个笔记 ID。
                    null      // 不使用 where 子句，因此不需要 where 参数。
            );

            // 返回调用者并跳过进一步处理。
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * 当用户单击显示列表中的笔记时调用此方法。
     *
     * 此方法处理 PICK（从提供者获取数据）或 GET_CONTENT（获取或创建数据）的传入操作。
     * 如果传入操作是 EDIT，此方法会发送新 Intent 以启动 NoteEditor。
     * @param l 包含被单击项目的 ListView
     * @param v 单个项目视图
     * @param position v 在显示列表中的位置
     * @param id 被单击项目的行 ID
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // 从传入 URI 和行 ID 构造新 URI
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // 从传入 Intent 获取操作
        String action = getIntent().getAction();

        // 处理笔记数据请求
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // 设置结果以返回给调用此 Activity 的组件。结果包含新 URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // 发送 Intent 以启动可以处理 ACTION_EDIT 的 Activity。
            // Intent 的数据是笔记 ID URI。效果是调用 NoteEdit。
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}

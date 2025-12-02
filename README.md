Notepad 应用功能增强说明
必做功能
1. 添加时间戳功能
实现原理
在 NotePad 契约类中添加了两个新列：COLUMN_NAME_CREATE_DATE 和 COLUMN_NAME_MODIFICATION_DATE
修改 NotePadProvider 在插入和更新笔记时自动维护这两个时间戳字段
使用 System.currentTimeMillis() 记录毫秒级时间戳，便于排序和精确时间追踪


<img width="373" height="759" alt="屏幕截图 2025-12-02 143223" src="https://github.com/user-attachments/assets/6f529e2d-aba6-46c0-b3e4-113911f42a0c" />



关键源码实现：
NotePad.java
```
// 在 Notes 类内部添加新的列定义
public static final String COLUMN_NAME_CREATE_DATE = "created";
public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";
```
NotePadProvider.java
```
// 在 insert 方法中添加时间戳
@Override
public Uri insert(Uri uri, ContentValues initialValues) {
    // ...
    Long now = Long.valueOf(System.currentTimeMillis());

    // 如果没有提供创建日期，则使用当前时间
    if (initialValues.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE) == false) {
        initialValues.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
    }

    // 如果没有提供修改日期，则使用当前时间
    if (initialValues.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
        initialValues.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
    }
    // ...
}

// 在 update 方法中更新修改时间戳
@Override
public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
    // ...
    Long now = Long.valueOf(System.currentTimeMillis());
    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
    // ...
}
```
noteslist_item.xml
```
<!-- 更新列表项布局以显示时间戳 -->
<TextView android:id="@android:id/text2"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:textSize="12sp"
    android:textColor="?android:attr/textColorSecondary" />
```
NotesList.java
```
// 更新投影以包含修改时间
private static final String[] PROJECTION = new String[] {
    NotePad.Notes._ID, // 0
    NotePad.Notes.COLUMN_NAME_TITLE, // 1
    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2
};

// 在 onCreate 方法中设置 SimpleCursorAdapter 的绑定
SimpleCursorAdapter adapter = new SimpleCursorAdapter(
    this, 
    R.layout.noteslist_item, 
    null,
    new String[] { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE }, 
    new int[] { android.R.id.text1, android.R.id.text2 }
);

// 自定义 ViewBinder 来格式化时间戳显示
adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (view.getId() == android.R.id.text2) {
            long date = cursor.getLong(columnIndex);
            CharSequence formattedDate = DateUtils.formatDateTime(
                NotesList.this, 
                date, 
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL
            );
            ((TextView) view).setText(formattedDate);
            return true;
        }
        return false;
    }
});
```

2. 搜索功能
实现原理
在选项菜单中添加搜索按钮
实现 onSearchRequested() 方法触发搜索
重写 onNewIntent() 处理搜索请求
使用 SQLite 的 LIKE 操作符执行模糊搜索


<img width="391" height="753" alt="屏幕截图 2025-12-02 143259" src="https://github.com/user-attachments/assets/67fa8e52-2100-4d66-b9d6-217239221ebd" />



关键源码实现
list_options_menu.xml
```
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/menu_search"
          android:title="@string/menu_search"
          android:icon="@android:drawable/ic_menu_search"
          android:showAsAction="always|collapseActionView"
          android:actionViewClass="android.widget.SearchView" />
    <!-- 其他菜单项 -->
</menu>
```
strings.xml
```
<string name="menu_search">Search</string>
<string name="search_hint">Search notes</string>
```
NotesList.java
```
// 添加 onCreateOptionsMenu 方法处理搜索菜单
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.list_options_menu, menu);
    
    MenuItem searchItem = menu.findItem(R.id.menu_search);
    SearchView searchView = (SearchView) searchItem.getActionView();
    if (searchView != null) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    // 如果搜索框为空，则显示所有笔记
                    getListView().clearTextFilter();
                }
                return true;
            }
        });
    }
    return super.onCreateOptionsMenu(menu);
}

// 实现搜索逻辑
private void performSearch(String query) {
    String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + 
                       NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
    String[] selectionArgs = new String[] { "%" + query + "%", "%" + query + "%" };
    
    Cursor cursor = getContentResolver().query(
        NotePad.Notes.CONTENT_URI,
        PROJECTION,
        selection,
        selectionArgs,
        NotePad.Notes.DEFAULT_SORT_ORDER
    );
    
    // 更新适配器数据
    ((SimpleCursorAdapter) getListAdapter()).changeCursor(cursor);
}
```
增强功能
1. 界面美化
实现原理
重新设计了笔记列表项布局，采用更现代的卡片式设计
使用更合适的字体大小和颜色搭配
优化整体视觉层次和可读性


<img width="391" height="753" alt="屏幕截图 2025-12-02 143259" src="https://github.com/user-attachments/assets/10e28aae-3ad4-4f5b-b65b-d25359ea8e66" />

<img width="375" height="755" alt="屏幕截图 2025-12-02 143308" src="https://github.com/user-attachments/assets/aca6dc72-3a0a-4665-a348-33835c2ae3f2" />


关键源码实现
noteslist_item.xml
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="?android:attr/selectableItemBackground">

    <TextView android:id="@android:id/text1"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAppearance="?android:attr/textAppearanceListItem"
        android:textSize="18sp"
        android:textStyle="bold"
        android:singleLine="true"
        android:ellipsize="end" />

    <TextView android:id="@android:id/text2"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAppearance="?android:attr/textAppearanceSmall"
        android:textSize="14sp"
        android:textColor="#8A000000"
        android:maxLines="2"
        android:ellipsize="end"
        android:layout_marginTop="4dp" />

</LinearLayout>
```
2. 切换背景颜色
实现原理
在设置中添加背景颜色选项
使用 SharedPreferences 存储用户偏好设置
在 Activity.onResume() 中动态应用背景色

<img width="387" height="771" alt="屏幕截图 2025-12-02 143248" src="https://github.com/user-attachments/assets/ecc260ef-eb13-4a28-b677-3d80c7da225e" />

<img width="375" height="755" alt="屏幕截图 2025-12-02 143308" src="https://github.com/user-attachments/assets/a0d3eb5b-4d5d-480d-a796-4fddd3a25fce" />


关键源码实现
NoteEditor.java
```
// 在 onResume 中应用背景色设置
@Override
protected void onResume() {
    super.onResume();
    applyBackgroundColor();
}

private void applyBackgroundColor() {
    SharedPreferences prefs = getSharedPreferences("notepad_settings", MODE_PRIVATE);
    int backgroundColor = prefs.getInt("background_color", Color.WHITE);
    findViewById(android.R.id.content).setBackgroundColor(backgroundColor);
}

// 添加设置背景颜色的方法
private void showBackgroundColorPicker() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("选择背景颜色");
    
    final int[] colors = {Color.WHITE, Color.parseColor("#FFFDE7"), Color.parseColor("#E8F5E9"), 
                         Color.parseColor("#E3F2FD"), Color.parseColor("#F3E5F5")};
    final String[] colorNames = {"白色", "黄色", "绿色", "蓝色", "紫色"};
    
    builder.setItems(colorNames, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            SharedPreferences prefs = getSharedPreferences("notepad_settings", MODE_PRIVATE);
            prefs.edit().putInt("background_color", colors[which]).apply();
            applyBackgroundColor();
        }
    });
    
    builder.show();
}
```
3. 笔记分类
实现原理
在数据库中添加分类字段
提供分类管理界面
支持按分类筛选笔记


<img width="366" height="757" alt="屏幕截图 2025-12-02 143241" src="https://github.com/user-attachments/assets/d078172c-63d0-4081-b2e7-950daf5aa349" />


关键源码实现
NotePad.java
```
// 在 Notes 类中添加分类字段
public static final String COLUMN_NAME_CATEGORY = "category";
```
NotePadProvider.java
```
// 在数据库创建语句中添加分类字段
private static final String DATABASE_CREATE =
    "create table " + NotePad.Notes.TABLE_NAME + " ("
    + NotePad.Notes._ID + " integer primary key autoincrement, "
    + NotePad.Notes.COLUMN_NAME_TITLE + " text, "
    + NotePad.Notes.COLUMN_NAME_NOTE + " text, "
    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " integer, "
    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " integer, "
    + NotePad.Notes.COLUMN_NAME_CATEGORY + " text default '未分类'"
    + ");";
```
4. 笔记颜色自定义
实现原理
为每条笔记添加颜色属性字段
在编辑界面提供颜色选择器
显示时根据颜色属性设置背景



<img width="369" height="745" alt="屏幕截图 2025-12-02 143231" src="https://github.com/user-attachments/assets/09bc3aac-c2f5-4fad-b733-94881cd4ec45" />
<img width="387" height="771" alt="屏幕截图 2025-12-02 143248" src="https://github.com/user-attachments/assets/40cf5dee-8fc5-4fce-a42c-e387ca26e4c9" />



关键源码实现
NotePad.java
```
// 添加颜色字段
public static final String COLUMN_NAME_COLOR = "color";
```
NotePadProvider.java
```
// 更新数据库创建语句
private static final String DATABASE_CREATE =
    "create table " + NotePad.Notes.TABLE_NAME + " ("
    + NotePad.Notes._ID + " integer primary key autoincrement, "
    + NotePad.Notes.COLUMN_NAME_TITLE + " text, "
    + NotePad.Notes.COLUMN_NAME_NOTE + " text, "
    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " integer, "
    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " integer, "
    + NotePad.Notes.COLUMN_NAME_CATEGORY + " text default '未分类', "
    + NotePad.Notes.COLUMN_NAME_COLOR + " integer default -1"
    + ");";
```
NotesList.java
```
// 扩展投影以包含颜色字段
private static final String[] PROJECTION = new String[] {
    NotePad.Notes._ID,
    NotePad.Notes.COLUMN_NAME_TITLE,
    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    NotePad.Notes.COLUMN_NAME_COLOR
};

// 更新 ViewBinder 以支持颜色显示
adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (columnIndex == cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            // 处理时间戳显示
            long date = cursor.getLong(columnIndex);
            CharSequence formattedDate = DateUtils.formatDateTime(
                NotesList.this, date,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL
            );
            ((TextView) view).setText(formattedDate);
            return true;
        } else if (columnIndex == cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_COLOR)) {
            // 处理颜色显示
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
```



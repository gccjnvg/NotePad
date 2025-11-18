/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.app.AlertDialog;

public class CategoryManagerActivity extends ListActivity {
    private static final int DELETE_ID = Menu.FIRST;
    private static final int INSERT_ID = Menu.FIRST + 1;

    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_list);

        fillData();
        registerForContextMenu(getListView());
    }

    private void fillData() {
        // 查询所有不同的分类
        mCursor = managedQuery(
                NotePad.Notes.CONTENT_URI,
                new String[]{NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_CATEGORY},
                NotePad.Notes.COLUMN_NAME_CATEGORY + " IS NOT NULL AND " + NotePad.Notes.COLUMN_NAME_CATEGORY + " != ''",
                null,
                NotePad.Notes.COLUMN_NAME_CATEGORY + " ASC"
        );

        String[] from = new String[]{NotePad.Notes.COLUMN_NAME_CATEGORY};
        int[] to = new int[]{android.R.id.text1};

        SimpleCursorAdapter categories = new SimpleCursorAdapter(this,
                R.layout.category_row, mCursor, from, to);
        setListAdapter(categories);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, "添加分类").setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case INSERT_ID:
                createCategory();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, "删除分类");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                deleteCategory(info.id);
                return true;
        }
        return false;
    }

    private void createCategory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加新分类");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String categoryName = input.getText().toString();
                if (!categoryName.isEmpty()) {
                    // 这里我们只是添加到UI，实际分类将在笔记创建时保存
                    // 可以考虑创建一个专门的分类表来存储分类信息
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void deleteCategory(long id) {
        // 删除分类逻辑 - 实际上我们会将该分类下的笔记设为无分类
        Uri noteUri = Uri.parse(NotePad.Notes.CONTENT_URI + "/" + id);
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, "");
        getContentResolver().update(noteUri, values, null, null);
        fillData();
    }
}

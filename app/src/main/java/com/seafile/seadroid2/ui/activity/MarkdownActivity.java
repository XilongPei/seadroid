package com.seafile.seadroid2.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.seafile.seadroid2.R;
import com.seafile.seadroid2.editor.EditorActivity;
import com.seafile.seadroid2.editor.EditorImageLoader;
import com.seafile.seadroid2.util.FileMimeUtils;
import com.seafile.seadroid2.util.Utils;
import com.yydcdut.markdown.MarkdownConfiguration;
import com.yydcdut.markdown.MarkdownProcessor;
import com.yydcdut.markdown.MarkdownTextView;
import com.yydcdut.markdown.loader.MDImageLoader;
import com.yydcdut.markdown.syntax.text.TextFactory;
import com.yydcdut.markdown.theme.ThemeSunburst;

import java.io.File;

/**
 * For showing markdown files
 */
public class MarkdownActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "MarkdownActivity";

    private MarkdownTextView markdownView;

    String path;
    private EditorImageLoader editorImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown);

        Intent intent = getIntent();
        path = intent.getStringExtra("path");

        if (path == null) return;

        markdownView = findViewById(R.id.markdownView);
        markdownView.setMovementMethod(LinkMovementMethod.getInstance());
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setOnMenuItemClickListener(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        File file = new File(path);
        if (!file.exists())
            return;

        String content = Utils.readFile(file);
        getSupportActionBar().setTitle(file.getName());

        editorImageLoader = new EditorImageLoader(this);
        markdown(markdownView, content, editorImageLoader);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBarToolbar().inflateMenu(R.menu.markdown_view_menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.edit_markdown:
                edit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void edit() {
        PackageManager pm = getPackageManager();

        // First try to find an activity who can handle markdown edit
        Intent editAsMarkDown = new Intent(Intent.ACTION_EDIT);
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT > 23) {
            uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(path));
            editAsMarkDown.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            uri = Uri.parse(path);
        }

        String mime = FileMimeUtils.getMimeType(new File(path));
        editAsMarkDown.setDataAndType(uri, mime);

        if ("text/plain".equals(mime)) {
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        } else if (pm.queryIntentActivities(editAsMarkDown, 0).size() > 0) {
            // Some activity can edit markdown
            startActivity(editAsMarkDown);
        } else {
            // No activity to handle markdown, take it as text
            Intent editAsText = new Intent(Intent.ACTION_EDIT);
            mime = "text/plain";
            editAsText.setDataAndType(uri, mime);

            try {
                startActivity(editAsText);
            } catch (ActivityNotFoundException e) {
                showShortToast(this, getString(R.string.activity_not_found));
            }
        }
    }

    private void markdown(final TextView textView, String content, MDImageLoader imageLoader) {

        int dip2px = Utils.dip2px(this, 200);
        MarkdownConfiguration markdownConfiguration = new MarkdownConfiguration.Builder(this)
                .setDefaultImageSize(dip2px, dip2px)
                .setBlockQuotesLineColor(0xffdddddd)
                .setHeader1RelativeSize(1.6f)
                .setHeader2RelativeSize(1.5f)
                .setHeader3RelativeSize(1.4f)
                .setHeader4RelativeSize(1.3f)
                .setHeader5RelativeSize(1.2f)
                .setHeader6RelativeSize(1.1f)
                .setHorizontalRulesColor(0xffdce1e7)
                .setCodeBgColor(0xfff5f7fa)
                .setTodoColor(0xffb8b8b8)
                .setTodoDoneColor(0xffb8b8b8)
                .setUnOrderListColor(0xff333333)
                .setRxMDImageLoader(imageLoader)
                .setHorizontalRulesHeight(1)
                .setLinkFontColor(0xff0852A7)
                .showLinkUnderline(false)
                .setTheme(new ThemeSunburst())
                .setOnLinkClickCallback((view, link) -> {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri content_url = Uri.parse(link);
                    intent.setData(content_url);
                    startActivity(intent);
                })
                .setOnTodoClickCallback((view, line, lineNumber) -> textView.getText())
                .build();
        MarkdownProcessor processor = new MarkdownProcessor(this);
        processor.factory(TextFactory.create());
        processor.config(markdownConfiguration);
        textView.setText(processor.parse(content));
    }

}

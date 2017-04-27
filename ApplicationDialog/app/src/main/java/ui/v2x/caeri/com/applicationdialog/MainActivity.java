package ui.v2x.caeri.com.applicationdialog;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private UICustomDialog customDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.custom_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                customDialog = new UICustomDialog(MainActivity.this);
                customDialog.setTitle("警告");
                customDialog.setMessage("确定退出应用?");
                customDialog.setYesOnclickListener("确定", new UICustomDialog.onYesOnclickListener() {
                    public void onYesClick() {
                        customDialog.updateTitle("111111");
                        customDialog.show();
                        Toast.makeText(MainActivity.this,"点击了--确定--按钮",Toast.LENGTH_LONG).show();
//                        customDialog.dismiss();
                    }
                });
                customDialog.setNoOnclickListener("取消", new UICustomDialog.onNoOnclickListener() {

                    public void onNoClick() {
                        Toast.makeText(MainActivity.this,"点击了--取消--按钮",Toast.LENGTH_LONG).show();
                        customDialog.dismiss();
                    }
                });
                customDialog.show();
            }
        });
    }
}

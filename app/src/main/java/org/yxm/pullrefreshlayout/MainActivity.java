package org.yxm.pullrefreshlayout;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PullRefreshLayout.OnRefreshListener {

    private PullRefreshLayout mPullRefreshLayout;
    //private ListView mListView;
    private RecyclerView mRecyclerView;
    //private ArrayAdapter<String> mAdapter;
    private MyAdapter adapter;
    private RecyclerView recyclerView;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                mPullRefreshLayout.refreshFinished();
            } else if (msg.what == 2) {
                List<String> list = new ArrayList<>();
                for (int i = 1; i <= 10; i++) {
                    list.add("item:" + i);
                }
                adapter.setDatas(list);
                mPullRefreshLayout.loadMoreFinished();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPullRefreshLayout = (PullRefreshLayout) findViewById(R.id.main_pullrefresh_layout);
        mPullRefreshLayout.setRefreshListener(this);
        initListView();
    }


    private void initListView() {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            list.add("item:" + i);
        }
        //mListView = (ListView) findViewById(R.id.main_listview);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MyAdapter(this);
        adapter.setDatas(list);
        //mListView.setAdapter(mAdapter);
        mRecyclerView.setAdapter(adapter);
    }


    @Override
    public void refreshFinished() {
        mHandler.sendEmptyMessageDelayed(1, 3 * 1000);
        Log.i("RecyclerView","refreshFinished");
    }

    @Override
    public void loadMoreFinished() {
        mHandler.sendEmptyMessageDelayed(2, 3 * 1000);
        Log.i("RecyclerView","loadMoreFinished");
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private List<String> list = new ArrayList<>();
        private LayoutInflater mLayoutInflater;

        public void setDatas(List<String> list) {
            this.list.addAll(list);
            notifyDataSetChanged();
        }

        public MyAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);

        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            MyViewHolder myViewHolder;
            View view = mLayoutInflater.inflate(R.layout.list_item, parent, false);
            myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {

            holder.textView.setText(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public MyViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.list_item_text);
            }
        }

    }

}

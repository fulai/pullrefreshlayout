package org.yxm.pullrefreshlayout;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HeaderViewListAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by yxm on 2017.02.26.
 */

public class MyAdapter extends RecyclerView.Adapter {
  private List<String> mDatas;
  private LayoutInflater mInflater;

  public MyAdapter(Context context, List<String> mDatas) {
    this.mDatas = mDatas;
    this.mInflater = LayoutInflater.from(context);
  }

  class MyViewHolder extends RecyclerView.ViewHolder {

    private TextView title;

    public MyViewHolder(View itemView) {
      super(itemView);
      title = (TextView) itemView.findViewById(R.id.list_item_text);
    }
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    MyViewHolder myViewHolder = new MyViewHolder(mInflater.inflate(R.layout.list_item, parent, false));
    return myViewHolder;
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    MyViewHolder myViewHolder = (MyViewHolder) holder;
    myViewHolder.title.setText(mDatas.get(position));
  }


  @Override
  public int getItemCount() {
    return mDatas.size();
  }
}

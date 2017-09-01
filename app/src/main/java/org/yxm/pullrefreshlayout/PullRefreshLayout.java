package org.yxm.pullrefreshlayout;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by dm on 2017.02.15.
 */

public class PullRefreshLayout extends ViewGroup {
    private View mHeader;
    private View mFooter;
    private TextView mHeaderText;
    private ImageView mHeaderArrow;
    private ProgressBar mHeaderProgressBar;
    private TextView mFooterText;
    private ProgressBar mFooterProgressBar;

    private int mLayoutContentWidth;
    private int mEffectiveHeaderWidth;
    private int mEffictiveFooterWidth;
    private int mlastMoveX;
    private float mlastRotation;
    private int mLastXIntercept;
    private int lastChildIndex;

    ObjectAnimator mAnimator;


    private Status mStatus = Status.NORMAL;

    private void updateStatus(Status status) {
        mStatus = status;
    }

    private enum Status {
        NORMAL, TRY_REFRESH, REFRESHING, TRY_LOADMORE, LOADING;
    }

    public interface OnRefreshListener {
        void refreshFinished();

        void loadMoreFinished();
    }

    private OnRefreshListener mRefreshListener;

    public void setRefreshListener(OnRefreshListener mRefreshListener) {
        this.mRefreshListener = mRefreshListener;
    }

    public PullRefreshLayout(Context context) {
        super(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // 当view的所有child从xml中被初始化后调用
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        lastChildIndex = getChildCount() - 1;
        addHeader();
        addFooter();
    }

    private void addHeader() {
        mHeader = LayoutInflater.from(getContext()).inflate(R.layout.pull_header_ext, this, false);
        addView(mHeader);
        mHeaderText = (TextView) findViewById(R.id.header_text);
        mHeaderProgressBar = (ProgressBar) findViewById(R.id.header_progressbar);
        mHeaderArrow = (ImageView) findViewById(R.id.header_arrow);
    }

    private void addFooter() {
        mFooter = LayoutInflater.from(getContext()).inflate(R.layout.pull_footer_ext, this, false);
        addView(mFooter);
        mFooterText = (TextView) findViewById(R.id.footer_text);
        mFooterProgressBar = (ProgressBar) findViewById(R.id.footer_progressbar);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mLayoutContentWidth = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mHeader) {
                child.layout(-child.getMeasuredWidth(), 0, 0, child.getMeasuredHeight());
                mEffectiveHeaderWidth = child.getWidth();
            } else if (child == mFooter) {
                child.layout(mLayoutContentWidth, 0, mLayoutContentWidth + child.getMeasuredWidth(), child
                        .getMeasuredHeight());
                mEffictiveFooterWidth = child.getWidth();
            } else {
                child.layout(mLayoutContentWidth, 0, child.getMeasuredWidth() + mLayoutContentWidth, child
                        .getMeasuredHeight());
                if (i < getChildCount()) {
                    if (child instanceof ScrollView) {
                        mLayoutContentWidth += getMeasuredWidth();
                        continue;
                    }
                    mLayoutContentWidth += child.getMeasuredWidth();
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();

        // 正在刷新或加载更多，避免重复
        if (mStatus == Status.REFRESHING || mStatus == Status.LOADING) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mlastMoveX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = mlastMoveX - x;
                // 一直在下拉
                if (getScrollX() <= 0 && dx <= 0) {
                    if (mStatus == Status.TRY_LOADMORE) {
                        scrollBy(dx / 30, 0);
                    } else {
                        scrollBy(dx / 3, 0);
                    }
                }
                // 一直在上拉
                else if (getScrollX() >= 0 && dx >= 0) {
                    if (mStatus == Status.TRY_REFRESH) {
                        scrollBy(dx / 30, 0);
                    } else {
                        scrollBy(dx / 3, 0);
                    }
                } else {
                    scrollBy(dx / 3, 0);
                }

                beforeRefreshing(dx);
                beforeLoadMore();

                break;
            case MotionEvent.ACTION_UP:
                // 下拉刷新，并且到达有效长度
                if (getScrollX() <= -mEffectiveHeaderWidth) {
                    releaseWithStatusRefresh();
                    if (mRefreshListener != null) {
                        mRefreshListener.refreshFinished();
                    }
                }
                // 上拉加载更多，达到有效长度
                else if (getScrollX() >= mEffictiveFooterWidth) {
                    releaseWithStatusLoadMore();
                    if (mRefreshListener != null) {
                        mRefreshListener.loadMoreFinished();
                    }
                } else {
                    releaseWithStatusTryRefresh();
                    releaseWithStatusTryLoadMore();
                }
                break;
        }

        mlastMoveX = x;
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = false;
        int x = (int) event.getX();

        if (mStatus == Status.REFRESHING || mStatus == Status.LOADING) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // 拦截时需要记录点击位置，不然下一次滑动会出错
                mlastMoveX = x;
                intercept = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {

                if (x > mLastXIntercept) {
                    View child = getChildAt(0);
                    intercept = getRefreshIntercept(child);

                    if (intercept) {
                        updateStatus(mStatus.TRY_REFRESH);
                    }
                } else if (x < mLastXIntercept) {
                    View child = getChildAt(lastChildIndex);
                    intercept = getLoadMoreIntercept(child);

                    if (intercept) {
                        updateStatus(mStatus.TRY_LOADMORE);
                    }
                } else {
                    intercept = false;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                intercept = false;
                break;
            }
        }

        mLastXIntercept = x;
        return intercept;
    }

    /*汇总判断 刷新和加载是否拦截*/
    private boolean getRefreshIntercept(View child) {
        boolean intercept = false;

        if (child instanceof AdapterView) {
            intercept = adapterViewRefreshIntercept(child);
        } else if (child instanceof ScrollView) {
            intercept = scrollViewRefreshIntercept(child);
        } else if (child instanceof RecyclerView) {
            intercept = recyclerViewRefreshIntercept(child);
        }
        return intercept;
    }


    private boolean getLoadMoreIntercept(View child) {
        boolean intercept = false;

        if (child instanceof AdapterView) {
            intercept = adapterViewLoadMoreIntercept(child);
        } else if (child instanceof ScrollView) {
            intercept = scrollViewLoadMoreIntercept(child);
        } else if (child instanceof RecyclerView) {
            intercept = recyclerViewLoadMoreIntercept(child);
        }
        return intercept;
    }
  /*汇总判断 刷新和加载是否拦截*/

    /*具体判断各种View是否应该拦截*/
    // 判断AdapterView下拉刷新是否拦截
    private boolean adapterViewRefreshIntercept(View child) {
        boolean intercept = true;
        AdapterView adapterChild = (AdapterView) child;
        if (adapterChild.getFirstVisiblePosition() != 0
                || adapterChild.getChildAt(0).getLeft() != 0) {
            intercept = false;
        }
        return intercept;
    }

    // 判断AdapterView加载更多是否拦截
    private boolean adapterViewLoadMoreIntercept(View child) {
        boolean intercept = false;
        AdapterView adapterChild = (AdapterView) child;
        if (adapterChild.getLastVisiblePosition() == adapterChild.getCount() - 1 &&
                (adapterChild.getChildAt(adapterChild.getChildCount() - 1).getRight() >= getMeasuredHeight())) {
            intercept = true;
        }
        return intercept;
    }

    // 判断ScrollView刷新是否拦截
    private boolean scrollViewRefreshIntercept(View child) {
        boolean intercept = false;
        if (child.getScrollX() <= 0) {
            intercept = true;
        }
        return intercept;
    }

    // 判断ScrollView加载更多是否拦截
    private boolean scrollViewLoadMoreIntercept(View child) {
        boolean intercept = false;
        ScrollView scrollView = (ScrollView) child;
        View scrollChild = scrollView.getChildAt(0);

        if (scrollView.getScrollX() >= (scrollChild.getWidth() - scrollView.getWidth())) {
            intercept = true;
        }
        return intercept;
    }

    // 判断RecyclerView刷新是否拦截
    private boolean recyclerViewRefreshIntercept(View child) {
        boolean intercept = false;

        RecyclerView recyclerView = (RecyclerView) child;
        if (recyclerView.computeHorizontalScrollOffset() <= 0) {
            intercept = true;
        }
        return intercept;
    }

    // 判断RecyclerView加载更多是否拦截
    private boolean recyclerViewLoadMoreIntercept(View child) {
        boolean intercept = false;

        RecyclerView recyclerView = (RecyclerView) child;
        if (recyclerView.computeHorizontalScrollExtent() + recyclerView.computeHorizontalScrollOffset()
                >= recyclerView.computeHorizontalScrollRange()) {
            intercept = true;
        }

        return intercept;
    }
  /*具体判断各种View是否应该拦截*/


    /*修改header和footer的状态*/
    public void beforeRefreshing(float dy) {
        //计算旋转角度
        int scrollX = Math.abs(getScrollX());
        scrollX = scrollX > mEffectiveHeaderWidth ? mEffectiveHeaderWidth : scrollX;
        float angle = (float) (scrollX * 1.0 / mEffectiveHeaderWidth * 180);
        mHeaderArrow.setRotation(angle);

        if (getScrollX() <= -mEffectiveHeaderWidth) {
            mHeaderText.setText("松开刷新");
        } else {
            mHeaderText.setText("右拉刷新");
        }
    }

    public void beforeLoadMore() {
        if (getScrollX() >= mEffectiveHeaderWidth) {
            mFooterText.setText("松开加载更多");
        } else {
            mFooterText.setText("左拉加载更多");
        }
    }

    public void refreshFinished() {
        scrollTo(0, 0);
        mHeaderText.setText("右拉刷新");
        mHeaderProgressBar.setVisibility(GONE);
        mHeaderArrow.setVisibility(VISIBLE);
        updateStatus(Status.NORMAL);
    }

    public void loadMoreFinished() {
        mFooterText.setText("加载中");
        mFooterProgressBar.setVisibility(GONE);
        scrollTo(0, 0);
        updateStatus(Status.NORMAL);
    }

    private void releaseWithStatusTryRefresh() {
        scrollBy(-getScrollX(), 0);
        mHeaderText.setText("右拉刷新");
        updateStatus(Status.NORMAL);
    }

    private void releaseWithStatusTryLoadMore() {
        scrollBy(-getScrollX(), 0);
        mFooterText.setText("左拉加载更多");
        updateStatus(Status.NORMAL);
    }

    private void releaseWithStatusRefresh() {
        scrollTo(-mEffectiveHeaderWidth, 0);
        mHeaderProgressBar.setVisibility(VISIBLE);
        mHeaderArrow.setVisibility(GONE);
        mHeaderText.setText("正在刷新");
        updateStatus(Status.REFRESHING);
    }

    private void releaseWithStatusLoadMore() {
        scrollTo(mEffictiveFooterWidth, 0);
        mFooterText.setText("正在加载");
        mFooterProgressBar.setVisibility(VISIBLE);
        updateStatus(Status.LOADING);
    }
  /*修改header和footer的状态*/


}

package com.haoyu.app.fragment;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

import com.haoyu.app.activity.CmtsMovementActivity;
import com.haoyu.app.adapter.CtmsMovementAdapter;
import com.haoyu.app.base.BaseFragment;
import com.haoyu.app.base.BaseResponseResult;
import com.haoyu.app.basehelper.BaseRecyclerAdapter;
import com.haoyu.app.entity.Paginator;
import com.haoyu.app.entity.TeachingMovementEntity;
import com.haoyu.app.entity.TeachingMovementListResult;
import com.haoyu.app.entity.TeachingRegistAtResult;
import com.haoyu.app.lego.teach.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.utils.Action;
import com.haoyu.app.utils.Constants;
import com.haoyu.app.utils.OkHttpClientManager;
import com.haoyu.app.view.LoadFailView;
import com.haoyu.app.view.LoadingView;
import com.haoyu.app.xrecyclerview.XRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import okhttp3.Request;

/**
 * 创建日期：2017/10/24 on 15:42
 * 描述:
 * 作者:马飞奔 Administrator
 */
public class CmtsMovChildFragment extends BaseFragment implements XRecyclerView.LoadingListener {
    @BindView(R.id.loadingView)
    LoadingView loadingView;
    @BindView(R.id.loadFailView)
    LoadFailView loadFailView;
    @BindView(R.id.xRecyclerView)
    XRecyclerView xRecyclerView;
    @BindView(R.id.emptyView)
    TextView emptyView;
    private List<TeachingMovementEntity> mDatas = new ArrayList<>();
    private CtmsMovementAdapter adapter;
    private boolean isRefresh, isLoadMore;
    private int page = 1;
    private String baseUrl;
    private int type = 1;
    private OnResponseListener onResponseListener;

    @Override
    public int createView() {
        return R.layout.fragment_teachstudy_child;
    }

    @Override
    public void initView(View view) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRecyclerView.setLayoutManager(layoutManager);
        adapter = new CtmsMovementAdapter(context, mDatas);
        xRecyclerView.setAdapter(adapter);
        xRecyclerView.setLoadingListener(this);
        emptyView.setText(getResources().getString(R.string.teach_active_emptylist));
        type = getArguments().getInt("type", 1);
        if (type == 1) {
            baseUrl = Constants.OUTRT_NET + "/m/movement?movementRelations[0].relation.id=cmts"
                    + "&movementRelations[0].relation.type=movement&orders=CREATE_TIME.DESC";
        } else {
            baseUrl = Constants.OUTRT_NET + "/m/movement?movementRelations[0].relation.id=cmts"
                    + "&movementRelations[0].relation.type=movement&orders=CREATE_TIME.DESC" + "&creator.id=" + getUserId();
        }
    }

    @Override
    public void initData() {
        String url = baseUrl + "&page=" + page;
        addSubscription(OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<TeachingMovementListResult>() {
            @Override
            public void onBefore(Request request) {
                if (isRefresh || isLoadMore) {
                    loadingView.setVisibility(View.GONE);
                } else {
                    loadingView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Request request, Exception e) {
                loadingView.setVisibility(View.GONE);
                if (isRefresh) {
                    xRecyclerView.refreshComplete(false);
                } else if (isLoadMore) {
                    page -= 1;
                    xRecyclerView.loadMoreComplete(false);
                } else {
                    xRecyclerView.setVisibility(View.GONE);
                    loadFailView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onResponse(TeachingMovementListResult response) {
                loadingView.setVisibility(View.GONE);
                if (response != null && response.getResponseData() != null
                        && response.getResponseData().getmMovements() != null
                        && response.getResponseData().getmMovements().size() > 0) {
                    updateUI(response.getResponseData().getmMovements(), response.getResponseData().getPaginator());
                } else {
                    if (isRefresh) {
                        xRecyclerView.refreshComplete(true);
                    } else if (isLoadMore) {
                        xRecyclerView.loadMoreComplete(true);
                    } else {
                        xRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }));
    }

    private void updateUI(List<TeachingMovementEntity> list, Paginator paginator) {
        if (xRecyclerView.getVisibility() != View.VISIBLE)
            xRecyclerView.setVisibility(View.VISIBLE);
        if (isRefresh) {
            mDatas.clear();
            xRecyclerView.refreshComplete(true);
        } else if (isLoadMore) {
            xRecyclerView.loadMoreComplete(true);
        }
        mDatas.addAll(list);
        adapter.notifyDataSetChanged();
        int totalCount = 0;
        if (paginator != null) {
            totalCount = paginator.getTotalCount();
            if (paginator.getHasNextPage())
                xRecyclerView.setLoadingMoreEnabled(true);
            else
                xRecyclerView.setLoadingMoreEnabled(false);
        } else {
            xRecyclerView.setLoadingMoreEnabled(false);
        }
        if (onResponseListener != null)
            onResponseListener.getTotalCount(totalCount);
    }

    @Override
    public void setListener() {
        loadFailView.setOnRetryListener(new LoadFailView.OnRetryListener() {
            @Override
            public void onRetry(View v) {
                initData();
            }
        });
        adapter.setOnButtonClick(new CtmsMovementAdapter.OnButtonClick() {
            @Override
            public void onClick(int position, TeachingMovementEntity entity) {
                Intent intent = new Intent(context, CmtsMovementActivity.class);
                intent.putExtra("entity", entity);
                startActivity(intent);
            }

            @Override
            public void register(int position, String activityId) {
                registerMovement(position, activityId);
            }

            @Override
            public void unregister(int position, String registerId) {
                unRegisterMovement(position, registerId);
            }
        });
        adapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerAdapter adapter, BaseRecyclerAdapter.RecyclerHolder holder, View view, int position) {
                int selected = position - 1;
                if (selected >= 0 && selected < mDatas.size()) {
                    TeachingMovementEntity entity = mDatas.get(selected);
                    Intent intent = new Intent(context, CmtsMovementActivity.class);
                    intent.putExtra("entity", entity);
                    startActivityForResult(intent, 1);
                }
            }
        });
    }

    private void registerMovement(final int position, String activityId) {
        String url = Constants.OUTRT_NET + "/m/movement/register";
        Map<String, String> map = new HashMap<>();
        map.put("movement.id", activityId);
        addSubscription(OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<TeachingRegistAtResult>() {
            @Override
            public void onBefore(Request request) {
                showTipDialog();
            }

            @Override
            public void onError(Request request, Exception e) {
                hideTipDialog();
                onNetWorkError();
            }

            @Override
            public void onResponse(TeachingRegistAtResult response) {
                hideTipDialog();
                if (response != null && response.getResponseData() != null) {
                    mDatas.get(position).getmMovementRegisters().add(response.getResponseData());
                    adapter.notifyDataSetChanged();
                } else {
                    toast("报名失败");
                }
            }
        }, map));
    }

    private void unRegisterMovement(final int position, String registerId) {
        String url = Constants.OUTRT_NET + "/m/movement/register/" + registerId;
        Map<String, String> map = new HashMap<>();
        map.put("_method", "delete");
        addSubscription(OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<BaseResponseResult>() {
            @Override
            public void onBefore(Request request) {
                showTipDialog();
            }

            @Override
            public void onError(Request request, Exception e) {
                hideTipDialog();
                onNetWorkError();
            }

            @Override
            public void onResponse(BaseResponseResult response) {
                hideTipDialog();
                if (response != null && response.getResponseCode() != null && response.getResponseCode().equals("00")) {
                    mDatas.get(position).getmMovementRegisters().clear();
                    adapter.notifyDataSetChanged();
                } else {
                    toast("取消报名失败");
                }
            }
        }, map));
    }

    @Override
    public void onRefresh() {
        isRefresh = true;
        isLoadMore = false;
        page = 1;
        initData();
    }

    @Override
    public void onLoadMore() {
        isRefresh = false;
        isLoadMore = true;
        page += 1;
        initData();
    }

    @Override
    public void obBusEvent(MessageEvent event) {
        if (event.getAction().equals(Action.DELETE_MOVEMENT)) {   //删除活动
            if (event.obj != null && event.obj instanceof TeachingMovementEntity) {
                TeachingMovementEntity entity = (TeachingMovementEntity) event.obj;
                mDatas.remove(entity);
                adapter.notifyDataSetChanged();
            }
            if (mDatas.size() == 0) {
                xRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        } else if (event.getAction().equals(Action.REGIST_MOVEMENT)) {   //活动报名
            if (event.obj != null && event.obj instanceof TeachingMovementEntity) {
                TeachingMovementEntity entity = (TeachingMovementEntity) event.obj;
                int selected = mDatas.indexOf(entity);
                if (selected >= 0) {
                    mDatas.set(selected, entity);
                    adapter.notifyDataSetChanged();
                }
            }
        } else if (event.getAction().equals(Action.UNREGIST_MOVEMENT)) {  //取消活动报名
            if (event.obj != null && event.obj instanceof TeachingMovementEntity) {
                TeachingMovementEntity entity = (TeachingMovementEntity) event.obj;
                int selected = mDatas.indexOf(entity);
                if (selected >= 0) {
                    mDatas.set(selected, entity);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    public interface OnResponseListener {
        void getTotalCount(int totalCount);
    }

    public void setOnResponseListener(OnResponseListener onResponseListener) {
        this.onResponseListener = onResponseListener;
    }
}
package com.speedata.uhf_simple.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.speedata.uhf_simple.R;

import java.util.List;

/**
 * 寻卡列表适配器
 * @author 张智超
 * @date 2019/3/7
 */
public class UhfCardAdapter extends ArrayAdapter<UhfCardBean> {
    private int newResourceId;

    public UhfCardAdapter(Context context, int resourceId, List<UhfCardBean> uhfCardBeanList) {
        super(context, resourceId, uhfCardBeanList);
        newResourceId = resourceId;
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        UhfCardBean uhfCardBean = getItem(position);
        @SuppressLint("ViewHolder") View view = LayoutInflater.from(getContext()).inflate(newResourceId, parent, false);
        TextView tvEpcSn = (TextView) view.findViewById(R.id.tv_epc_sn);
        TextView tvEpcName = (TextView) view.findViewById(R.id.tv_epc_name);

        assert uhfCardBean != null;
        tvEpcSn.setText(uhfCardBean.getTvValid());
        tvEpcName.setText(uhfCardBean.getTvepc());
        return view;
    }
}

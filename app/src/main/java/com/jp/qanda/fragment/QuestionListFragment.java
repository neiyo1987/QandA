package com.jp.qanda.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.jp.qanda.R;
import com.jp.qanda.TableConstants;
import com.jp.qanda.follow.QuestionUtil;
import com.jp.qanda.question.QuestionDetailActivity;
import com.jp.qanda.vo.Question;
import com.jp.qanda.vo.User;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author jpwang
 * @since 6/2/16
 */
public abstract class QuestionListFragment extends Fragment {
    private DatabaseReference databaseReference;
    private FirebaseRecyclerAdapter<Question, QuestionViewHolder> adapter;

    @BindView(R.id.questionList)
    RecyclerView questionList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_question_list, container, false);
        ButterKnife.bind(this, rootView);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LinearLayoutManager manager = new LinearLayoutManager(this.getActivity());
        adapter = new FirebaseRecyclerAdapter<Question, QuestionViewHolder>(Question.class, R.layout.question_list_item, QuestionViewHolder.class, getQuery(databaseReference)) {
            @Override
            protected void populateViewHolder(QuestionViewHolder viewHolder, Question model, int position) {
                final String questionKey = getRef(position).getKey();
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(QuestionDetailActivity.createIntent(getActivity(), questionKey));
                    }
                });
                viewHolder.bindData(model);
            }
        };
        questionList.setLayoutManager(manager);
        questionList.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.cleanup();
        }
    }

    protected abstract Query getQuery(DatabaseReference databaseReference);

    static class QuestionViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.userNameAndTitleTv)
        TextView userNameAndTitleTv;

        @BindView(R.id.questionTv)
        TextView questionTv;

        @BindView(R.id.timeTv)
        TextView timeTv;

        @BindView(R.id.userSecretListeners)
        TextView userSecretListenersTv;

        @BindView(R.id.userAskedOrAnsweredTv)
        TextView userAskedOrAnsweredTv;

        private String currentUserId;

        private DatabaseReference userTable;

        public QuestionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            userTable = FirebaseDatabase.getInstance().getReference(TableConstants.TABLE_USERS);
        }

        public void bindData(Question question) {
            if (question.answer != null) {
                userAskedOrAnsweredTv.setText(R.string.question_answered);
                if (question.to.equals(currentUserId)) {
                    userNameAndTitleTv.setText("Me");
                } else {
                    updateUserNameAndTitle(question.to);
                }
                timeTv.setText(QuestionUtil.getDisplayTime(System.currentTimeMillis() - question.answer.timestamp));
                userSecretListenersTv.setText(itemView.getContext().getString(R.string.user_secret_listening_count, question.answer.secretListeners));
            } else {
                userAskedOrAnsweredTv.setText(R.string.question_asked);
                updateUserNameAndTitle(question.from);
                timeTv.setText(QuestionUtil.getDisplayTime(System.currentTimeMillis() - question.timestamp));
                userSecretListenersTv.setText(itemView.getContext().getString(R.string.user_secret_listening_count, 0));
            }
            questionTv.setText(question.content);

        }

        private void updateUserNameAndTitle(final String userId) {
            userNameAndTitleTv.setTag(userId);
            userNameAndTitleTv.setText("");
            userTable.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (userId.equals(userNameAndTitleTv.getTag())) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            userNameAndTitleTv.setText(user.username + (user.title != null && user.title.isEmpty() ? " | " + user.title : ""));
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}

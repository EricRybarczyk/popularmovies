package com.example.ericrybarczyk.popularmovies;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.ericrybarczyk.popularmovies.model.Movie;
import com.example.ericrybarczyk.popularmovies.utils.MovieAppConstants;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieHolder> {

    private List<Movie> movieList;
    private final Context parentContext;
    private final MovieAdapterOnClickHandler movieItemClickHandler;
    private final String sortPreference;

    public interface MovieAdapterOnClickHandler {
        void onClick(int movieId);
    }

    MovieAdapter(Context context, MovieAdapterOnClickHandler clickHandler, String sortPreference) {
        parentContext = context;
        movieItemClickHandler = clickHandler;
        this.sortPreference = sortPreference;
    }


    public void setMovieData(List<Movie> movieList) {
        this.movieList = movieList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.movie_list_item, parent, false);
        return new MovieHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieHolder holder, int position) {
        Movie movie = movieList.get(position);

        if (this.sortPreference.equals(parentContext.getResources().getString(R.string.pref_sort_favorite_value))) {
            // GET IMAGE FILE FROM FILE SYSTEM
            String filename = MovieAppConstants.LOCAL_POSTER_PREFIX + String.valueOf(movie.getId());
            File imageFile = new File(parentContext.getFilesDir(), filename);
            Picasso.with(parentContext)
                    .load(imageFile)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.placeholder_movie_black_18dp)
                    .into(holder.movieImageView);
        } else {
            Picasso.with(parentContext)
                    .load(movie.getImagePath())
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.placeholder_movie_black_18dp)
                    .into(holder.movieImageView);
        }
    }

    @Override
    public int getItemCount() {
        if (movieList == null) {
            return 0;
        }
        return movieList.size();
    }


    public class MovieHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.iv_movie_item) protected ImageView movieImageView;

        MovieHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        @OnClick
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            int movieId = movieList.get(adapterPosition).getId();
            movieItemClickHandler.onClick(movieId);
        }
    }

}

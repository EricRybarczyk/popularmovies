package com.example.ericrybarczyk.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ericrybarczyk.popularmovies.data.DateConverter;
import com.example.ericrybarczyk.popularmovies.data.FavoriteMoviesContract;
import com.example.ericrybarczyk.popularmovies.data.FavoriteMoviesDbHelper;
import com.example.ericrybarczyk.popularmovies.model.Movie;
import com.example.ericrybarczyk.popularmovies.utils.FontManager;
import com.example.ericrybarczyk.popularmovies.utils.MovieAppConstants;
import com.example.ericrybarczyk.popularmovies.utils.NetworkChecker;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;


public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Movie>, View.OnClickListener {

    private static final int MOVIE_LOADER = 5291;
    private static final String TAG = MovieService.class.getName();
    private FavoriteMoviesDbHelper dbHelper;
    private Movie loadedMovie;
    private boolean isFavorite;

    @BindView(R.id.movie_image) protected ImageView imageView;
    @BindView(R.id.rating_stars) protected RatingBar ratingBar;
    @BindView(R.id.release_date_value) protected TextView releaseDate;
    @BindView(R.id.movie_title_value) protected TextView movieTitle;
    @BindView(R.id.movie_overview_value) protected TextView movieOverview;
    @BindView(R.id.rating_description) protected TextView ratingDescription;
    @BindView(R.id.rating_label) protected TextView ratingLabel;
    @BindView(R.id.release_date_label) protected TextView releaseDateLabel;
    @BindView(R.id.trailers_text_icon) protected TextView trailersTextIcon;
    @BindView(R.id.trailers_text_value) protected TextView trailersLabel;
    @BindView(R.id.reviews_text_icon) protected TextView reviewsTextIcon;
    @BindView(R.id.reviews_text_value) protected TextView reviewsLabel;
    @BindView(R.id.favorite_text_icon) protected TextView favoriteTextIcon;
    @BindView(R.id.favorite_text_value) protected TextView favoriteLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);
        dbHelper = new FavoriteMoviesDbHelper(this);


        // TODO - if viewing a movie from favorites database, no need for network check
        // need to adjust logic flow so this is only called when NOT displaying a favorite from db
        if (!NetworkChecker.isNetworkConnected(this)) {
            Log.e(TAG, "No network available");
            NetworkChecker.getNoNetworkToastMessage(this).show();
            return;
        }

        Intent starter = getIntent();
        if (starter != null) {
            int movieId;
            if (starter.hasExtra(MovieAppConstants.KEY_MOVIE_ID)) {
                movieId = starter.getIntExtra(MovieAppConstants.KEY_MOVIE_ID, 0);
            } else {
                Log.e(TAG, "Missing expected data: " + MovieAppConstants.KEY_MOVIE_ID);
                movieId = -1;
            }

            this.isFavorite = movieIsFavorite(movieId);

            loadMovieDetail(movieId, false);

            // trailers and reviews widget setup
            trailersTextIcon.setTypeface(FontManager.getTypeface(this, FontManager.FONTAWESOME_SOLID));
            reviewsTextIcon.setTypeface(FontManager.getTypeface(this, FontManager.FONTAWESOME_SOLID));

            // adjust appearance if current movie is marked as user favorite
            setFavoriteViewAppearance();

            setClickHandlers();
        }
    }


    private void setClickHandlers() {
        trailersTextIcon.setOnClickListener(this);
        trailersLabel.setOnClickListener(this);
        reviewsTextIcon.setOnClickListener(this);
        reviewsLabel.setOnClickListener(this);
        favoriteTextIcon.setOnClickListener(this);
        favoriteLabel.setOnClickListener(this);
    }

    private void loadMovieDetail(int movieId, boolean refresh) {
        Bundle bundle = new Bundle();
        bundle.putInt(MovieAppConstants.KEY_MOVIE_ID, movieId);

        LoaderManager loaderManager = getSupportLoaderManager();

        if (refresh) {
            loaderManager.restartLoader(MOVIE_LOADER, bundle, this);
            Log.d(TAG, "loadMovieDetail - restartLoader");
        } else {
            loaderManager.initLoader(MOVIE_LOADER, bundle, this);
            Log.d(TAG, "loadMovieDetail - initLoader");
        }

    }

    @NonNull
    @Override
    public Loader<Movie> onCreateLoader(int id, Bundle args) {
        int movieId = args.getInt(MovieAppConstants.KEY_MOVIE_ID);
        return new MovieAsyncTaskLoader(this, movieId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Movie> loader, Movie data) {

        loadedMovie = data; // set a reference for the click event handlers

        // see if we got the error movie object back
        if (data.getId() == -1) {
            movieTitle.setText(R.string.error_movie_title);
            movieOverview.setText(R.string.error_movie_description);
            imageView.setVisibility(View.GONE);
            ratingLabel.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
            releaseDateLabel.setVisibility(View.GONE);
            releaseDate.setVisibility(View.GONE);
            return;
        }

        ratingBar.setRating((float)(data.getUserRating() / 2));
        releaseDate.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(data.getReleaseDate()));
        movieTitle.setText(data.getTitle());
        movieOverview.setText(data.getOverview());
        String ratingString = (new DecimalFormat("0.##")).format( (data.getUserRating() / 2) );
        String ratingDescriptionText = getString(R.string.rating_description_text, ratingString);
        ratingDescription.setText(ratingDescriptionText); // N.n out of 5 stars

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int maxImageWidth = (displayMetrics.widthPixels / 2);
        imageView.setMaxWidth(maxImageWidth);

        Picasso.with(this)
                .load(data.getImagePath())
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.placeholder_movie_black_18dp)
                .into(imageView);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Movie> loader) {
        // not doing anything here, just required by LoaderCallback<> interface
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.trailers_text_icon:
            case R.id.trailers_text_value:
                Class trailersActivity = TrailerListActivity.class;
                Intent trailersIntent = new Intent(this, trailersActivity);
                trailersIntent.putExtra(MovieAppConstants.KEY_MOVIE_ID, loadedMovie.getId());
                trailersIntent.putExtra(MovieAppConstants.KEY_MOVIE_TITLE, loadedMovie.getTitle());
                startActivity(trailersIntent);
                break;
            case R.id.reviews_text_icon:
            case R.id.reviews_text_value:
                Class reviewsActivity = ReviewListActivity.class;
                Intent reviewsIntent = new Intent(this, reviewsActivity);
                reviewsIntent.putExtra(MovieAppConstants.KEY_MOVIE_ID, loadedMovie.getId());
                reviewsIntent.putExtra(MovieAppConstants.KEY_MOVIE_TITLE, loadedMovie.getTitle());
                startActivity(reviewsIntent);
                break;
            case R.id.favorite_text_icon:
            case R.id.favorite_text_value:
                toggleFavorite();
                //Toast.makeText(this, "Favorite!!!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setFavoriteViewAppearance() {
        if (this.isFavorite) {
            favoriteTextIcon.setTypeface(FontManager.getTypeface(this, FontManager.FONTAWESOME_SOLID));
            favoriteTextIcon.setTextColor(getResources().getColor(R.color.colorFavoriteIcon));
            favoriteLabel.setTextColor(getResources().getColor(R.color.colorFavoriteLabel));
        } else {
            favoriteTextIcon.setTypeface(FontManager.getTypeface(this, FontManager.FONTAWESOME_REGULAR));
            favoriteTextIcon.setTextColor(getResources().getColor(R.color.colorPrimary));
            favoriteLabel.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    private boolean movieIsFavorite(int movieId) {
        SQLiteDatabase favoritesDb = dbHelper.getReadableDatabase();
        boolean isFavorite = false;
        int evalId = -1;

        try {
            Cursor cursor = favoritesDb.query(
                    FavoriteMoviesContract.FavoriteMoviesEntry.TABLE_NAME,
                    new String[] {FavoriteMoviesContract.FavoriteMoviesEntry._ID},
                    null,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext()) {
                evalId = cursor.getInt(0);
                if (evalId == movieId) {
                    isFavorite = true;
                    break;
                }
            }

            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return isFavorite;
    }

    private void toggleFavorite() {    // TODO - implement toggleFavorite()
        SQLiteDatabase favoritesDb = dbHelper.getWritableDatabase();
        if (this.isFavorite) {
            String where = FavoriteMoviesContract.FavoriteMoviesEntry._ID + "=" + String.valueOf(this.loadedMovie.getId());
            favoritesDb.delete(FavoriteMoviesContract.FavoriteMoviesEntry.TABLE_NAME, where, null);
            // TODO - delete local file of poster image
        } else {
            ContentValues values = new ContentValues();
            values.put(FavoriteMoviesContract.FavoriteMoviesEntry._ID, this.loadedMovie.getId());
            values.put(FavoriteMoviesContract.FavoriteMoviesEntry.COLUMN_TITLE, this.loadedMovie.getTitle());
            // TODO - save the REAL local file path info
            values.put(FavoriteMoviesContract.FavoriteMoviesEntry.COLUMN_IMAGE_PATH_LOCAL, this.loadedMovie.getImagePath());
            values.put(FavoriteMoviesContract.FavoriteMoviesEntry.COLUMN_IMAGE_PATH_REMOTE, this.loadedMovie.getImagePath());
            values.put(FavoriteMoviesContract.FavoriteMoviesEntry.COLUMN_OVERVIEW, this.loadedMovie.getOverview());
            values.put(FavoriteMoviesContract.FavoriteMoviesEntry.COLUMN_USER_RATING, this.loadedMovie.getUserRating());
            values.put(FavoriteMoviesContract.FavoriteMoviesEntry.COLUMN_RELEASE_DATE, DateConverter.toTimestamp(this.loadedMovie.getReleaseDate()));
            favoritesDb.insert(FavoriteMoviesContract.FavoriteMoviesEntry.TABLE_NAME, null, values);
        }
        // state is saved, so flip the local indicator and update the display
        this.isFavorite = (!this.isFavorite);
        setFavoriteViewAppearance();
    }

}

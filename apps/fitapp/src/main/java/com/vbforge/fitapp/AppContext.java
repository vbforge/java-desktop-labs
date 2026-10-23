package com.vbforge.fitapp;

import com.vbforge.fitapp.db.AppPaths;
import com.vbforge.fitapp.db.Database;
import com.vbforge.fitapp.db.ExerciseDao;
import com.vbforge.fitapp.db.LoginHistoryDao;
import com.vbforge.fitapp.db.UserDao;
import com.vbforge.fitapp.db.WorkoutDao;
import com.vbforge.fitapp.service.AuthService;
import com.vbforge.fitapp.service.ConfigStore;
import com.vbforge.fitapp.service.WorkoutService;
import com.vbforge.fitapp.session.SessionManager;

import java.sql.SQLException;

/** Wires the object graph once; passed to the UI instead of static global state. */
public record AppContext(Database db, SessionManager session, AuthService auth,
                         WorkoutService workouts, ConfigStore config) {

    public static AppContext create() throws SQLException {
        return build(Database.openDefault(), new ConfigStore(AppPaths.configFile()));
    }

    public static AppContext build(Database db, ConfigStore config) {
        SessionManager session = new SessionManager();
        AuthService auth = new AuthService(new UserDao(db), new LoginHistoryDao(db), session);
        WorkoutService workouts = new WorkoutService(new WorkoutDao(db), new ExerciseDao(db), session);
        return new AppContext(db, session, auth, workouts, config);
    }
}

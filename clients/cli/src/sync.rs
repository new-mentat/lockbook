use lockbook_core::{
    calculate_work, execute_work, get_account, set_last_synced, CalculateWorkError, GetAccountError,
};

use crate::utils::{exit_with, exit_with_no_account, get_config};
use crate::{NETWORK_ISSUE, UNEXPECTED_ERROR};
use lockbook_core::model::work_unit::WorkUnit;
use std::io;
use std::io::Write;

pub fn sync() {
    let account = match get_account(&get_config()) {
        Ok(account) => account,
        Err(err) => match err {
            GetAccountError::NoAccount => exit_with_no_account(),
            GetAccountError::UnexpectedError(msg) => exit_with(&msg, UNEXPECTED_ERROR),
        },
    };

    let mut work_calculated = match calculate_work(&get_config()) {
        Ok(work) => work,
        Err(err) => match err {
            CalculateWorkError::NoAccount => exit_with_no_account(),
            CalculateWorkError::CouldNotReachServer => {
                exit_with("Could not reach server!", NETWORK_ISSUE)
            }
            CalculateWorkError::UnexpectedError(msg) => exit_with(&msg, UNEXPECTED_ERROR),
        },
    };

    while !work_calculated.work_units.is_empty() {
        for work_unit in work_calculated.work_units {
            print!(
                "{}",
                match work_unit.clone() {
                    WorkUnit::LocalChange { metadata } =>
                        format!("Syncing: {} to server", metadata.name),
                    WorkUnit::ServerChange { metadata } =>
                        format!("Syncing: {} from server", metadata.name),
                }
            );
            let _ = io::stdout().flush();
            match execute_work(&get_config(), &account, work_unit) {
                Ok(_) => println!("Success."),
                Err(error) => eprintln!("Failed: {:?}", error),
            }
        }

        work_calculated = match calculate_work(&get_config()) {
            Ok(work) => work,
            Err(err) => match err {
                CalculateWorkError::NoAccount => exit_with_no_account(),
                CalculateWorkError::CouldNotReachServer => {
                    exit_with("Could not reach server!", NETWORK_ISSUE)
                }
                CalculateWorkError::UnexpectedError(msg) => exit_with(&msg, UNEXPECTED_ERROR),
            },
        };
    }

    match set_last_synced(
        &get_config(),
        work_calculated.most_recent_update_from_server,
    ) {
        Ok(_) => {}
        Err(_) => {}
    }

    println!("Sync complete.");
}

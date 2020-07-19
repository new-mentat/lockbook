//
//  ControllerView.swift
//  ios_client
//
//  Created by Raayan Pillai on 4/12/20.
//  Copyright © 2020 Lockbook. All rights reserved.
//

import SwiftUI

struct ControllerView: View {
    @ObservedObject var loginManager: LoginManager

    var body: some View {
        if let account = loginManager.account {
            let coordinator = (try? Coordinator(lockbookApi: loginManager.lockbookApi, account: account))!
            return AnyView(VStack {
                FileBrowserView(coordinator: coordinator)
                ProgressWidget(coordinator: coordinator)
                    .frame(height: 20)
                    .padding()
            })
        } else {
            return AnyView(WelcomeView(loginManager: loginManager))
        }
    }
}

struct ControllerView_Previews: PreviewProvider {
    static var previews: some View {
        ControllerView(loginManager: LoginManager())
    }
}

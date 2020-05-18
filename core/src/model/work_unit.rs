use crate::model::api::FileMetadata as ServerFileMetadata;
use crate::model::client_file_metadata::ClientFileMetadata;
use serde::Serialize;

#[derive(Serialize, Debug, Clone)]
pub enum WorkUnit {
    /// No action needs to be taken for this file
    Nop,

    /// File was created locally and doesn't exist anywhere else, push this file to the server
    PushNewFile(ClientFileMetadata),

    /// Server has changed metadata, lookup the corresponding ClientMetadata and apply Server's
    /// metadata transformations.
    UpdateLocalMetadata(ServerFileMetadata),

    /// Goto s3 and grab the new contents of this file, update metadata if successful
    PullFileContent(ServerFileMetadata),

    /// File and metadata is safe to delete locally now
    DeleteLocally(ClientFileMetadata),

    /// Inform the server of your metadata change
    PushMetadata(ClientFileMetadata),

    /// Inform the server of a local file edit. If push fails due to a conflict, attempt PullMergePush
    /// TODO we don't have a new metadata version or a new file content version without another getUpdates call
    PushFileContent(ClientFileMetadata),

    /// Inform the server of a file deletion. If successful, delete the file locally.
    PushDelete(ClientFileMetadata),

    /// Pull the new file, decrypt it, decrypt the file locally, merge them, and push the resulting file.
    PullMergePush(ServerFileMetadata),

    /// Compare with local metadata, merge non-conflicting changes, send changes to server,
    /// if successful update metadata locally.
    MergeMetadataAndPushMetadata(ServerFileMetadata),
}

pub fn get_verb(work: &WorkUnit) -> String {
    match work {
        WorkUnit::Nop => "Doing nothing".to_string(),
        WorkUnit::PushNewFile(client) => format!("Pushing file: {}", client.file_name),
        WorkUnit::UpdateLocalMetadata(server) => {
            format!("Updating metadata for: {}", server.file_name)
        }
        WorkUnit::PullFileContent(server) => format!("Pulling file: {}", server.file_name),
        WorkUnit::DeleteLocally(client) => format!("Deleting file: {}", client.file_name),
        WorkUnit::PushMetadata(client) => format!("Pushing metadata: {}", client.file_name),
        WorkUnit::PushFileContent(client) => format!("Pushing file: {}", client.file_name),
        WorkUnit::PushDelete(client) => format!("Sending delete: {}", client.file_name),
        WorkUnit::PullMergePush(server) => format!("Merging file: {}", server.file_name),
        WorkUnit::MergeMetadataAndPushMetadata(server) => {
            format!("Merging metadata for: {}", server.file_name)
        }
    }
}

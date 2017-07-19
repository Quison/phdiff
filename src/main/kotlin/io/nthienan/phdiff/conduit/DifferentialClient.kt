package io.nthienan.phdiff.conduit

import io.nthienan.phdiff.differential.Diff
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Date

/**
 * Created on 19-Jul-17.
 * @author nthienan
 */
class DifferentialClient(val conduitClient: ConduitClient) {

    /**
     * Posts a comment to a differential
     * @param revisionID the revision ID (e.g. "D1234" without the "D")
     * @param message the content of the comment
     * @param silent whether or not to trigger an email
     * @param action phabricator comment action, e.g. 'resign', 'reject', 'none'
     * @return the Conduit API response
     * @throws IOException if there is a network error talking to Conduit
     * @throws ConduitException if any error is experienced talking to Conduit
     */
    @Throws(IOException::class, ConduitException::class)
    fun postComment(revisionID: String, message: String, silent: Boolean, action: String): JSONObject {
        var params = JSONObject()
        params.put("revision_id", revisionID)
        params.put("action", action)
        params.put("message", message)
        params.put("silent", silent)
        return conduitClient.perform("differential.createcomment", params)
    }

    /**
     * Fetch a differential from Conduit
     * @return the Conduit API response
     * @throws IOException if there is a network error talking to Conduit
     * @throws ConduitException if any error is experienced talking to Conduit
     */
    @Throws(IOException::class, ConduitException::class)
    fun fetchDiff(diffID: String): Diff {
        val params = JSONObject().put("ids", arrayOf(diffID))
        val query = conduitClient.perform("differential.querydiffs", params)
        val response: JSONObject
        try {
            response = query.getJSONObject("result")
        } catch (e: JSONException) {
            throw ConduitException(
                String.format("No 'result' object found in conduit call: (%s) %s",
                    e.message, query.toString(2)))
        }

        val diffJsonObj = response.getJSONObject(diffID)
        val diff = Diff()
        diff.id = diffJsonObj.get("id").toString()
        diff.revisionId = diffJsonObj.getString("revisionID")
//        diff.branch = diffJsonObj.getString("branch")
        diff.dateCreated = Date(diffJsonObj.getLong("dateCreated"))
        diff.dateModified = Date(diffJsonObj.getLong("dateModified"))
        return diff

    }

    /**
     * Post a comment on the differential
     * @param revisionID the revision ID (e.g. "D1234" without the "D")
     * @param message the string message to post
     * @return the Conduit API response
     * @throws IOException if there is a network error talking to Conduit
     * @throws ConduitException if any error is experienced talking to Conduit
     */
    @Throws(ConduitException::class, IOException::class)
    fun postComment(revisionID: String, message: String): JSONObject {
        return postComment(revisionID, message, true, "none")
    }

    /**
     * Fetch the commit message for the revision. This isn't available on the diff, so it requires a separate query.
     * @param revisionID The ID of the revision, e.g. for "D123" this would be "123"
     * @return A \n-separated string of the commit message
     * @throws ConduitException
     * @throws IOException
     */
    @Throws(ConduitException::class, IOException::class)
    fun getCommitMessage(revisionID: String): String {
        val params = JSONObject().put("revision_id", revisionID)
        val query = conduitClient.perform("differential.getcommitmessage", params)

        // NOTE: When you run this with `arc call-conduit dfferential.getcommitmessage` (from the command-line),
        // it comes back as "response". But it's "result" when running through this conduit API.
        return query.getString("result")
    }

}
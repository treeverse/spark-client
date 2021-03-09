# lakeFS Spark Client

Utilize the power of Spark to interact with the metadata on lakeFS. Possible use-cases include:

* Create a DataFrame for listing the objects in a specific commit or branch.
* Compute changes between two commits.
* Export your data for consumption outside lakeFS.
* Import existing data into lakeFS.
* [Something with Retention]

## Getting Started
Start Spark Shell / PySpark with the `--packages` flag:

```bash
spark-shell --packages io.treeverse:lakefs-spark-client_2.12:0.1.0-SNAPSHOT
```

Alternatively, you can download the Jar from [here]()

## Configuration

1. To read metadata from lakeFS, the client should be configured with your lakeFS endpoint and credentials, using the following Hadoop configurations:

    | Configuration                        | Description                                                  |
    |--------------------------------------|--------------------------------------------------------------|
    | `spark.hadoop.lakefs.api.url`        | lakeFS API endpoint, e.g: `http://lakefs.example.com/api/v1` |
    | `spark.hadoop.lakefs.api.access_key` | The access key to use for fetching metadata from lakeFS      |
    | `spark.hadoop.lakefs.api.secret_key` | Corresponding lakeFS secret key                              |

1. The client will also directly interact with your storage using Hadoop FileSystem. Therefore, your Spark session must be able to access the underlying storage of your lakeFS repository.  

## Example

Get a DataFrame for listing all objects in a commit:

```scala
import io.treeverse.clients.LakeFSContext

val commitID = "a1b2c3d4"
val df = LakeFSContext.newDF(spark, "example-repo", commitID)

df.show
```



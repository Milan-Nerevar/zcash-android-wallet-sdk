package cash.z.ecc.android.sdk.internal

import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.internal.ext.getSha1Hash
import cash.z.ecc.android.sdk.internal.ext.listFilesSuspend
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.fixture.SaplingParamToolFixture
import cash.z.ecc.fixture.SaplingParamsFixture
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SaplingParamToolBasicTest {

    @Before
    fun setup() {
        // clear the param files
        runBlocking {
            SaplingParamsFixture.clearAllFilesFromDirectory(SaplingParamsFixture.DESTINATION_DIRECTORY)
            SaplingParamsFixture.clearAllFilesFromDirectory(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY)
        }
    }

    @Test
    @SmallTest
    fun init_sapling_param_tool_test() = runTest {
        val spendSaplingParams = SaplingParamsFixture.new()
        val outputSaplingParams = SaplingParamsFixture.new(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME,
            SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE,
            SaplingParamsFixture.OUTPUT_FILE_HASH
        )

        val saplingParamTool = SaplingParamTool(
            SaplingParamToolProperties(
                emptyList(),
                SaplingParamsFixture
                    .DESTINATION_DIRECTORY,
                SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY
            )
        )

        // we inject params files to let the ensureParams() finish successfully without executing its extended operation
        // like fetchParams, etc.
        SaplingParamsFixture.createFile(File(spendSaplingParams.destinationDirectory, spendSaplingParams.fileName))
        SaplingParamsFixture.createFile(File(outputSaplingParams.destinationDirectory, outputSaplingParams.fileName))

        saplingParamTool.ensureParams(spendSaplingParams.destinationDirectory)
    }

    @Test
    @SmallTest
    fun init_and_get_params_destination_dir_test() = runTest {
        val destDir = SaplingParamTool.new(getAppContext()).properties.paramsDirectory

        assertNotNull(destDir)
        assertEquals(
            SaplingParamsFixture.DESTINATION_DIRECTORY.absolutePath,
            destDir.absolutePath,
            "Failed to validate init operation's destination directory."
        )
    }

    @Test
    @MediumTest
    fun move_files_from_legacy_destination_test() = runTest {
        SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY.mkdirs()
        val spendFile = File(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, SaplingParamsFixture.SPEND_FILE_NAME)
        val outputFile = File(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, SaplingParamsFixture.OUTPUT_FILE_NAME)

        // now we inject params files to the legacy location to be "moved" to the preferred location
        SaplingParamsFixture.createFile(spendFile)
        SaplingParamsFixture.createFile(outputFile)

        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, spendFile))
        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, outputFile))
        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, spendFile))
        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, outputFile))

        // we need to use modified array of sapling parameters to pass through the SHA1 hashes validation
        val destDir = SaplingParamTool.initAndGetParamsDestinationDir(
            SaplingParamToolFixture.new(
                saplingParamsFiles = SaplingParamToolFixture.SAPLING_PARAMS_FILES
                    .also {
                        it[0].fileHash = spendFile.getSha1Hash()
                        it[1].fileHash = outputFile.getSha1Hash()
                    }
            )
        )

        assertEquals(
            SaplingParamsFixture.DESTINATION_DIRECTORY.absolutePath,
            destDir.absolutePath
        )

        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, spendFile))
        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, outputFile))
        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, spendFile))
        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, outputFile))
    }

    private suspend fun isFileInPlace(directory: File, file: File): Boolean {
        return directory.listFilesSuspend()?.any { it.name == file.name } ?: false
    }

    @Test
    @MediumTest
    fun ensure_params_exception_thrown_test() = runTest {
        val saplingParamTool = SaplingParamTool(
            SaplingParamToolFixture.new(
                saplingParamsFiles = SaplingParamToolFixture.SAPLING_PARAMS_FILES
                    .also {
                        it[0].fileName = "test_file_0"
                        it[1].fileName = "test_file_1"
                    }
            )
        )

        // now we inject params files to the preferred location to pass through the check missing files phase
        SaplingParamsFixture.createFile(
            File(
                SaplingParamToolFixture.SAPLING_PARAMS_FILES[0].destinationDirectory,
                SaplingParamToolFixture.SAPLING_PARAMS_FILES[0].fileName
            )
        )
        SaplingParamsFixture.createFile(
            File(
                SaplingParamToolFixture.SAPLING_PARAMS_FILES[1].destinationDirectory,
                SaplingParamToolFixture.SAPLING_PARAMS_FILES[1].fileName
            )
        )

        // the ensure params block should fail in validation phase, because we use a different params file names
        assertFailsWith<TransactionEncoderException.MissingParamsException> {
            saplingParamTool.ensureParams(SaplingParamToolFixture.PARAMS_DIRECTORY)
        }
    }
}

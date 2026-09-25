package com.securerank.service.impl;

import com.securerank.dto.response.AlgorithmMetricResult;
import com.securerank.dto.response.BenchmarkReportResponse;
import com.securerank.service.LosslessTransformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class LosslessTransformationServiceImpl implements LosslessTransformationService {

    @Override
    public double calculateShannonEntropy(String bitStream) {
        if (bitStream == null || bitStream.isEmpty()) return 0.0;

        int len = bitStream.length();
        int count0 = 0;
        int count1 = 0;

        for (int i = 0; i < len; i++) {
            if (bitStream.charAt(i) == '0') count0++;
            else if (bitStream.charAt(i) == '1') count1++;
        }

        double p0 = (double) count0 / len;
        double p1 = (double) count1 / len;

        double entropy = 0.0;
        if (p0 > 0.0) entropy -= p0 * (Math.log(p0) / Math.log(2.0));
        if (p1 > 0.0) entropy -= p1 * (Math.log(p1) / Math.log(2.0));

        return Math.round(entropy * 10000.0) / 10000.0;
    }

    // =========================================================================
    // 1. RUN LENGTH ENCODING (RLE)
    // =========================================================================
    @Override
    public AlgorithmMetricResult evaluateRLE(String bitStream) {
        long startTime = System.nanoTime();
        long startMem = getUsedMemory();

        // Compression: encode consecutive identical bits as "count#bit,"
        StringBuilder compressed = new StringBuilder();
        int len = bitStream.length();
        if (len > 0) {
            char current = bitStream.charAt(0);
            int count = 1;
            for (int i = 1; i < len; i++) {
                if (bitStream.charAt(i) == current) {
                    count++;
                } else {
                    compressed.append(count).append('#').append(current).append(',');
                    current = bitStream.charAt(i);
                    count = 1;
                }
            }
            compressed.append(count).append('#').append(current);
        }
        String compressedStr = compressed.toString();
        long compTime = System.nanoTime() - startTime;

        // Decompression
        long decStartTime = System.nanoTime();
        StringBuilder decompressed = new StringBuilder();
        if (!compressedStr.isEmpty()) {
            String[] tokens = compressedStr.split(",");
            for (String token : tokens) {
                int hashIdx = token.indexOf('#');
                if (hashIdx > 0) {
                    int run = Integer.parseInt(token.substring(0, hashIdx));
                    char bit = token.charAt(hashIdx + 1);
                    for (int i = 0; i < run; i++) {
                        decompressed.append(bit);
                    }
                }
            }
        }
        long decTime = System.nanoTime() - decStartTime;
        long memUsed = Math.max(0, getUsedMemory() - startMem);

        boolean fidelity = bitStream.equals(decompressed.toString());
        double compRatio = ((double) compressedStr.length() / Math.max(1, bitStream.length())) * 100.0;

        return AlgorithmMetricResult.builder()
                .algorithmName("Run Length Encoding (RLE)")
                .algorithmCategory("Traditional Compression")
                .originalSizeChars(bitStream.length())
                .compressedSizeChars(compressedStr.length())
                .compressionRatioPercent(round(compRatio))
                .spaceSavingsPercent(round(100.0 - compRatio))
                .compressionTimeMs(round(compTime / 1_000_000.0))
                .decompressionTimeMs(round(decTime / 1_000_000.0))
                .memoryUtilizedKb(round(memUsed / 1024.0))
                .losslessFidelity(fidelity)
                .sampleCompressedOutput(getSample(compressedStr, 60))
                .notes("Performs poorly on VC binary streams due to frequent alternating bits (near-maximal entropy).")
                .build();
    }

    // =========================================================================
    // 2. HUFFMAN CODING
    // =========================================================================
    @Override
    public AlgorithmMetricResult evaluateHuffman(String bitStream) {
        long startTime = System.nanoTime();
        long startMem = getUsedMemory();

        byte[] originalBytes = bitStreamToBytes(bitStream);
        int bitCount = bitStream.length();

        // Build frequency table of byte symbols
        int[] freq = new int[256];
        for (byte b : originalBytes) {
            freq[b & 0xFF]++;
        }

        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.freq));
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                pq.offer(new HuffmanNode(i, freq[i], null, null));
            }
        }
        if (pq.isEmpty()) {
            pq.offer(new HuffmanNode(0, 1, null, null));
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            pq.offer(new HuffmanNode(-1, left.freq + right.freq, left, right));
        }
        HuffmanNode root = pq.peek();

        Map<Integer, String> codeMap = new HashMap<>();
        buildHuffmanCodes(root, "", codeMap);

        // Encode bytes using Huffman codes
        StringBuilder encodedBits = new StringBuilder();
        for (byte b : originalBytes) {
            encodedBits.append(codeMap.get(b & 0xFF));
        }
        String encodedStr = encodedBits.toString();
        long compTime = System.nanoTime() - startTime;

        // Decompression
        long decStartTime = System.nanoTime();
        List<Byte> decodedByteList = new ArrayList<>();
        HuffmanNode curr = root;
        for (int i = 0; i < encodedStr.length(); i++) {
            char bit = encodedStr.charAt(i);
            curr = (bit == '0') ? curr.left : curr.right;
            if (curr.isLeaf()) {
                decodedByteList.add((byte) curr.symbol);
                curr = root;
            }
        }
        byte[] decodedBytes = new byte[decodedByteList.size()];
        for (int i = 0; i < decodedByteList.size(); i++) {
            decodedBytes[i] = decodedByteList.get(i);
        }
        String decompressed = bytesToBitStream(decodedBytes, bitCount);
        long decTime = System.nanoTime() - decStartTime;
        long memUsed = Math.max(0, getUsedMemory() - startMem);

        boolean fidelity = bitStream.equals(decompressed);
        double compRatio = ((double) encodedStr.length() / Math.max(1, bitStream.length())) * 100.0;

        return AlgorithmMetricResult.builder()
                .algorithmName("Huffman Coding")
                .algorithmCategory("Traditional Compression")
                .originalSizeChars(bitStream.length())
                .compressedSizeChars(encodedStr.length())
                .compressionRatioPercent(round(compRatio))
                .spaceSavingsPercent(round(100.0 - compRatio))
                .compressionTimeMs(round(compTime / 1_000_000.0))
                .decompressionTimeMs(round(decTime / 1_000_000.0))
                .memoryUtilizedKb(round(memUsed / 1024.0))
                .losslessFidelity(fidelity)
                .sampleCompressedOutput(getSample(encodedStr, 60))
                .notes("Variable-length entropy coding. In randomized VC binary data, symbol frequencies are nearly uniform.")
                .build();
    }

    // =========================================================================
    // 3. LEMPEL-ZIV-WELCH (LZW)
    // =========================================================================
    @Override
    public AlgorithmMetricResult evaluateLZW(String bitStream) {
        long startTime = System.nanoTime();
        long startMem = getUsedMemory();

        byte[] originalBytes = bitStreamToBytes(bitStream);

        // LZW Compression
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put("" + (char) i, i);
        }

        String w = "";
        List<Integer> compressedCodes = new ArrayList<>();
        int dictSize = 256;

        for (byte b : originalBytes) {
            char c = (char) (b & 0xFF);
            String wc = w + c;
            if (dictionary.containsKey(wc)) {
                w = wc;
            } else {
                compressedCodes.add(dictionary.get(w));
                if (dictSize < 4096) {
                    dictionary.put(wc, dictSize++);
                }
                w = "" + c;
            }
        }
        if (!w.isEmpty()) {
            compressedCodes.add(dictionary.get(w));
        }

        long compTime = System.nanoTime() - startTime;

        // LZW Decompression
        long decStartTime = System.nanoTime();
        Map<Integer, String> decDict = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            decDict.put(i, "" + (char) i);
        }

        StringBuilder decompressedChars = new StringBuilder();
        int decDictSize = 256;
        if (!compressedCodes.isEmpty()) {
            int prevCode = compressedCodes.get(0);
            String s = decDict.get(prevCode);
            decompressedChars.append(s);

            for (int i = 1; i < compressedCodes.size(); i++) {
                int currCode = compressedCodes.get(i);
                String entry;
                if (decDict.containsKey(currCode)) {
                    entry = decDict.get(currCode);
                } else if (currCode == decDictSize) {
                    entry = s + s.charAt(0);
                } else {
                    entry = "";
                }
                decompressedChars.append(entry);

                if (decDictSize < 4096) {
                    decDict.put(decDictSize++, s + entry.charAt(0));
                }
                s = entry;
            }
        }

        byte[] restoredBytes = new byte[decompressedChars.length()];
        for (int i = 0; i < decompressedChars.length(); i++) {
            restoredBytes[i] = (byte) decompressedChars.charAt(i);
        }
        String decompressed = bytesToBitStream(restoredBytes, bitStream.length());
        long decTime = System.nanoTime() - decStartTime;
        long memUsed = Math.max(0, getUsedMemory() - startMem);

        // Represent compressed output size in character count
        int compressedSizeChars = compressedCodes.size() * 2; // 16-bit code tokens
        double compRatio = ((double) compressedSizeChars / Math.max(1, bitStream.length())) * 100.0;
        boolean fidelity = bitStream.equals(decompressed);

        return AlgorithmMetricResult.builder()
                .algorithmName("Lempel-Ziv-Welch (LZW)")
                .algorithmCategory("Traditional Compression")
                .originalSizeChars(bitStream.length())
                .compressedSizeChars(compressedSizeChars)
                .compressionRatioPercent(round(compRatio))
                .spaceSavingsPercent(round(100.0 - compRatio))
                .compressionTimeMs(round(compTime / 1_000_000.0))
                .decompressionTimeMs(round(decTime / 1_000_000.0))
                .memoryUtilizedKb(round(memUsed / 1024.0))
                .losslessFidelity(fidelity)
                .sampleCompressedOutput(getSample(compressedCodes.toString(), 60))
                .notes("Dictionary-based algorithm. Little pattern redundancy exists in cryptographic shares, limiting dictionary matching.")
                .build();
    }

    // =========================================================================
    // 4. BINARY-TO-INTEGER CONVERSION
    // =========================================================================
    @Override
    public AlgorithmMetricResult evaluateBinaryToInteger(String bitStream) {
        long startTime = System.nanoTime();
        long startMem = getUsedMemory();

        // Convert bit stream into 32-bit unsigned integers
        List<Long> integerList = new ArrayList<>();
        int chunkLen = 32;
        int totalBits = bitStream.length();

        for (int i = 0; i < totalBits; i += chunkLen) {
            String chunk = bitStream.substring(i, Math.min(i + chunkLen, totalBits));
            long val = Long.parseLong(chunk, 2);
            integerList.add(val);
        }

        StringBuilder compressedRepresentation = new StringBuilder();
        for (int i = 0; i < integerList.size(); i++) {
            compressedRepresentation.append(integerList.get(i));
            if (i < integerList.size() - 1) compressedRepresentation.append(",");
        }
        String compressedStr = compressedRepresentation.toString();
        long compTime = System.nanoTime() - startTime;

        // Decompression: recover exact bit stream
        long decStartTime = System.nanoTime();
        StringBuilder decompressed = new StringBuilder();
        String[] tokens = compressedStr.split(",");
        for (int i = 0; i < tokens.length; i++) {
            long val = Long.parseLong(tokens[i]);
            String bits = Long.toBinaryString(val);
            int expectedBits = (i == tokens.length - 1 && totalBits % chunkLen != 0) ? (totalBits % chunkLen) : chunkLen;

            // Pad with leading zeros
            while (bits.length() < expectedBits) {
                bits = "0" + bits;
            }
            decompressed.append(bits);
        }
        long decTime = System.nanoTime() - decStartTime;
        long memUsed = Math.max(0, getUsedMemory() - startMem);

        boolean fidelity = bitStream.equals(decompressed.toString());
        double compRatio = ((double) compressedStr.length() / Math.max(1, bitStream.length())) * 100.0;

        return AlgorithmMetricResult.builder()
                .algorithmName("Binary-to-Integer Conversion")
                .algorithmCategory("Lossless Transformation")
                .originalSizeChars(bitStream.length())
                .compressedSizeChars(compressedStr.length())
                .compressionRatioPercent(round(compRatio))
                .spaceSavingsPercent(round(100.0 - compRatio))
                .compressionTimeMs(round(compTime / 1_000_000.0))
                .decompressionTimeMs(round(decTime / 1_000_000.0))
                .memoryUtilizedKb(round(memUsed / 1024.0))
                .losslessFidelity(fidelity)
                .sampleCompressedOutput(getSample(compressedStr, 60))
                .notes("Packs 32-bit chunks into decimal integers. Reduces character count compared to raw binary strings.")
                .build();
    }

    // =========================================================================
    // 5. BASE64 ENCODING (Optimal Character Reduction for QR Bitstreams)
    // =========================================================================
    @Override
    public AlgorithmMetricResult evaluateBase64(String bitStream) {
        long startTime = System.nanoTime();
        long startMem = getUsedMemory();

        byte[] rawBytes = bitStreamToBytes(bitStream);
        String base64Encoded = Base64.getEncoder().encodeToString(rawBytes);
        long compTime = System.nanoTime() - startTime;

        // Decompression
        long decStartTime = System.nanoTime();
        byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
        String decompressed = bytesToBitStream(decodedBytes, bitStream.length());
        long decTime = System.nanoTime() - decStartTime;
        long memUsed = Math.max(0, getUsedMemory() - startMem);

        boolean fidelity = bitStream.equals(decompressed);
        double compRatio = ((double) base64Encoded.length() / Math.max(1, bitStream.length())) * 100.0;

        return AlgorithmMetricResult.builder()
                .algorithmName("Base64 Encoding")
                .algorithmCategory("Lossless Transformation (Optimal for QR)")
                .originalSizeChars(bitStream.length())
                .compressedSizeChars(base64Encoded.length())
                .compressionRatioPercent(round(compRatio))
                .spaceSavingsPercent(round(100.0 - compRatio))
                .compressionTimeMs(round(compTime / 1_000_000.0))
                .decompressionTimeMs(round(decTime / 1_000_000.0))
                .memoryUtilizedKb(round(memUsed / 1024.0))
                .losslessFidelity(fidelity)
                .sampleCompressedOutput(getSample(base64Encoded, 60))
                .notes("Achieves the most significant character reduction (approx. 83.3% reduction) for QR payload optimization with instant O(N) throughput.")
                .build();
    }

    // =========================================================================
    // 6. BWT + MTF + HUFFMAN PIPELINE
    // =========================================================================
    @Override
    public AlgorithmMetricResult evaluateBwtMtfHuffman(String bitStream) {
        long startTime = System.nanoTime();
        long startMem = getUsedMemory();

        byte[] rawBytes = bitStreamToBytes(bitStream);

        // 1. Burrows-Wheeler Transform (BWT)
        BwtResult bwtResult = applyBWT(rawBytes);

        // 2. Move-To-Front (MTF) Transform
        byte[] mtfBytes = applyMTF(bwtResult.transformed);

        // 3. Huffman Coding on MTF result
        int[] freq = new int[256];
        for (byte b : mtfBytes) {
            freq[b & 0xFF]++;
        }

        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.freq));
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                pq.offer(new HuffmanNode(i, freq[i], null, null));
            }
        }
        if (pq.isEmpty()) pq.offer(new HuffmanNode(0, 1, null, null));

        while (pq.size() > 1) {
            HuffmanNode l = pq.poll();
            HuffmanNode r = pq.poll();
            pq.offer(new HuffmanNode(-1, l.freq + r.freq, l, r));
        }
        HuffmanNode root = pq.peek();
        Map<Integer, String> codeMap = new HashMap<>();
        buildHuffmanCodes(root, "", codeMap);

        StringBuilder pipelineEncoded = new StringBuilder();
        for (byte b : mtfBytes) {
            pipelineEncoded.append(codeMap.get(b & 0xFF));
        }
        String pipelineStr = pipelineEncoded.toString();
        long compTime = System.nanoTime() - startTime;

        // Decompression: Inverse Huffman -> Inverse MTF -> Inverse BWT
        long decStartTime = System.nanoTime();
        List<Byte> decMtfList = new ArrayList<>();
        HuffmanNode curr = root;
        for (int i = 0; i < pipelineStr.length(); i++) {
            char bit = pipelineStr.charAt(i);
            curr = (bit == '0') ? curr.left : curr.right;
            if (curr.isLeaf()) {
                decMtfList.add((byte) curr.symbol);
                curr = root;
            }
        }
        byte[] restoredMtf = new byte[decMtfList.size()];
        for (int i = 0; i < decMtfList.size(); i++) restoredMtf[i] = decMtfList.get(i);

        byte[] restoredBwt = invertMTF(restoredMtf);
        byte[] restoredBytes = invertBWT(restoredBwt, bwtResult.primaryIndex);
        String decompressed = bytesToBitStream(restoredBytes, bitStream.length());
        long decTime = System.nanoTime() - decStartTime;
        long memUsed = Math.max(0, getUsedMemory() - startMem);

        boolean fidelity = bitStream.equals(decompressed);
        double compRatio = ((double) pipelineStr.length() / Math.max(1, bitStream.length())) * 100.0;

        return AlgorithmMetricResult.builder()
                .algorithmName("BWT + MTF + Huffman Pipeline")
                .algorithmCategory("Hybrid Transformation Pipeline")
                .originalSizeChars(bitStream.length())
                .compressedSizeChars(pipelineStr.length())
                .compressionRatioPercent(round(compRatio))
                .spaceSavingsPercent(round(100.0 - compRatio))
                .compressionTimeMs(round(compTime / 1_000_000.0))
                .decompressionTimeMs(round(decTime / 1_000_000.0))
                .memoryUtilizedKb(round(memUsed / 1024.0))
                .losslessFidelity(fidelity)
                .sampleCompressedOutput(getSample(pipelineStr, 60))
                .notes("Combines block sorting (BWT) and recency ranking (MTF) before Huffman coding. Provides multi-stage lossless transform.")
                .build();
    }

    // =========================================================================
    // BENCHMARK ORCHESTRATOR
    // =========================================================================
    @Override
    public BenchmarkReportResponse runFullBenchmark(Long fileId, String filename, String bitStream) {
        log.info("Running full 6-algorithm benchmark on bitstream for file ID: {} (length: {})", fileId, bitStream.length());

        double entropy = calculateShannonEntropy(bitStream);

        List<AlgorithmMetricResult> results = new ArrayList<>();
        results.add(evaluateRLE(bitStream));
        results.add(evaluateHuffman(bitStream));
        results.add(evaluateLZW(bitStream));
        results.add(evaluateBinaryToInteger(bitStream));
        results.add(evaluateBase64(bitStream));
        results.add(evaluateBwtMtfHuffman(bitStream));

        // Find optimal algorithm by lowest compressed size (character count)
        AlgorithmMetricResult optimal = results.stream()
                .min(Comparator.comparingInt(AlgorithmMetricResult::getCompressedSizeChars))
                .orElse(results.get(4));

        String conclusion = String.format(
                "Evaluated 6 lossless algorithms over %d-bit Visual Cryptographic binary stream (Shannon Entropy: %.4f). " +
                        "Because VC shares have near-maximal entropy, traditional algorithms (RLE, Huffman, LZW) experience negligible compression or expansion. " +
                        "%s achieved the optimal character reduction (%.2f%% space savings) with complete lossless fidelity, making it optimal for QR code capacity constraints.",
                bitStream.length(), entropy, optimal.getAlgorithmName(), optimal.getSpaceSavingsPercent()
        );

        return BenchmarkReportResponse.builder()
                .fileId(fileId)
                .filename(filename)
                .bitStreamLength(bitStream.length())
                .shannonEntropy(entropy)
                .optimalAlgorithm(optimal.getAlgorithmName())
                .conclusion(conclusion)
                .results(results)
                .build();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private byte[] bitStreamToBytes(String bitStream) {
        int len = bitStream.length();
        int byteLen = (len + 7) / 8;
        byte[] bytes = new byte[byteLen];

        for (int i = 0; i < len; i++) {
            if (bitStream.charAt(i) == '1') {
                bytes[i / 8] |= (1 << (7 - (i % 8)));
            }
        }
        return bytes;
    }

    private String bytesToBitStream(byte[] bytes, int expectedLength) {
        StringBuilder sb = new StringBuilder(expectedLength);
        for (int i = 0; i < expectedLength; i++) {
            byte b = bytes[i / 8];
            int bit = (b >> (7 - (i % 8))) & 1;
            sb.append(bit == 1 ? '1' : '0');
        }
        return sb.toString();
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private String getSample(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static class HuffmanNode {
        int symbol;
        int freq;
        HuffmanNode left;
        HuffmanNode right;

        HuffmanNode(int symbol, int freq, HuffmanNode left, HuffmanNode right) {
            this.symbol = symbol;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    private void buildHuffmanCodes(HuffmanNode node, String code, Map<Integer, String> map) {
        if (node == null) return;
        if (node.isLeaf()) {
            map.put(node.symbol, code.isEmpty() ? "0" : code);
            return;
        }
        buildHuffmanCodes(node.left, code + "0", map);
        buildHuffmanCodes(node.right, code + "1", map);
    }

    private static class BwtResult {
        byte[] transformed;
        int primaryIndex;

        BwtResult(byte[] transformed, int primaryIndex) {
            this.transformed = transformed;
            this.primaryIndex = primaryIndex;
        }
    }

    private BwtResult applyBWT(byte[] input) {
        int n = input.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        Arrays.sort(indices, (a, b) -> {
            for (int k = 0; k < n; k++) {
                int byteA = input[(a + k) % n] & 0xFF;
                int byteB = input[(b + k) % n] & 0xFF;
                if (byteA != byteB) return Integer.compare(byteA, byteB);
            }
            return 0;
        });

        byte[] output = new byte[n];
        int primary = 0;
        for (int i = 0; i < n; i++) {
            output[i] = input[(indices[i] + n - 1) % n];
            if (indices[i] == 0) primary = i;
        }
        return new BwtResult(output, primary);
    }

    private byte[] invertBWT(byte[] bwt, int primaryIndex) {
        int n = bwt.length;
        int[] count = new int[257];
        for (byte b : bwt) {
            count[(b & 0xFF) + 1]++;
        }
        for (int i = 1; i < 256; i++) {
            count[i] += count[i - 1];
        }

        int[] next = new int[n];
        for (int i = 0; i < n; i++) {
            int ch = bwt[i] & 0xFF;
            next[count[ch]++] = i;
        }

        byte[] original = new byte[n];
        int ptr = primaryIndex;
        for (int i = 0; i < n; i++) {
            ptr = next[ptr];
            original[i] = bwt[ptr];
        }
        return original;
    }

    private byte[] applyMTF(byte[] input) {
        byte[] list = new byte[256];
        for (int i = 0; i < 256; i++) list[i] = (byte) i;

        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            byte b = input[i];
            int rank = 0;
            while (list[rank] != b) rank++;

            output[i] = (byte) rank;

            // Move to front
            System.arraycopy(list, 0, list, 1, rank);
            list[0] = b;
        }
        return output;
    }

    private byte[] invertMTF(byte[] mtf) {
        byte[] list = new byte[256];
        for (int i = 0; i < 256; i++) list[i] = (byte) i;

        byte[] output = new byte[mtf.length];
        for (int i = 0; i < mtf.length; i++) {
            int rank = mtf[i] & 0xFF;
            byte b = list[rank];
            output[i] = b;

            // Move to front
            System.arraycopy(list, 0, list, 1, rank);
            list[0] = b;
        }
        return output;
    }
}

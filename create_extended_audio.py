#!/usr/bin/env python3
import json
import subprocess
import random

def create_long_audio():
    # Create a 15+ minute file by repeating patterns
    segments = []
    current_time = 0.0
    target_duration = 15 * 60  # 15 minutes
    
    while current_time < target_duration:
        # Random audio duration (0.3 to 3.0 seconds)
        audio_duration = round(random.uniform(0.3, 3.0), 1)
        frequency = random.randint(200, 800)
        
        segments.append({
            "startTime": current_time,
            "endTime": current_time + audio_duration,
            "body": f"Audio content: {frequency}Hz tone - {audio_duration}s",
            "speaker": "",
            "type": "audio",
            "frequency": frequency,
            "duration": audio_duration
        })
        current_time += audio_duration
        
        if current_time < target_duration:
            # Random silence (100ms to 400ms)
            silence_duration = round(random.uniform(0.1, 0.4), 3)
            microseconds = int(silence_duration * 1000000)
            
            segments.append({
                "startTime": current_time,
                "endTime": current_time + silence_duration,
                "body": f"SILENCE: {int(silence_duration * 1000)}ms ({microseconds} microseconds)",
                "speaker": "",
                "type": "silence",
                "duration": silence_duration
            })
            current_time += silence_duration
    
    return segments

def create_audio_chunks(segments, chunk_size=50):
    """Create audio in chunks to avoid command line length issues"""
    chunks = []
    for i in range(0, len(segments), chunk_size):
        chunk = segments[i:i + chunk_size]
        chunks.append(chunk)
    return chunks

def create_chunk_audio(chunk, chunk_num):
    """Create audio file for a chunk of segments"""
    inputs = []
    filter_inputs = []
    
    for i, segment in enumerate(chunk):
        if segment["type"] == "audio":
            inputs.extend(["-f", "lavfi", "-i", f"sine=frequency={segment['frequency']}:duration={segment['duration']}"])
        else:  # silence
            inputs.extend(["-f", "lavfi", "-i", f"anullsrc=channel_layout=mono:sample_rate=48000:duration={segment['duration']}"])
        
        filter_inputs.append(f"[{i}:a]")
    
    concat_filter = "".join(filter_inputs) + f"concat=n={len(chunk)}:v=0:a=1[out]"
    filename = f"chunk_{chunk_num:03d}.mp3"
    
    cmd = ["ffmpeg", "-y"] + inputs + ["-filter_complex", concat_filter, "-map", "[out]", "-c:a", "libmp3lame", "-b:a", "128k", filename]
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode == 0:
        return filename
    else:
        print(f"Error creating chunk {chunk_num}: {result.stderr}")
        return None

def concatenate_chunks(chunk_files):
    """Concatenate all chunk files into final audio file"""
    # Create file list for ffmpeg concat
    with open("file_list.txt", "w") as f:
        for chunk_file in chunk_files:
            f.write(f"file '{chunk_file}'\n")
    
    cmd = ["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", "file_list.txt", "-c", "copy", "test_skip_silence_extended_random.mp3"]
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.returncode == 0

if __name__ == "__main__":
    print("Generating segments for 15+ minutes of audio...")
    segments = create_long_audio()
    
    print(f"Generated {len(segments)} segments")
    print(f"Total duration: {segments[-1]['endTime']:.1f} seconds ({segments[-1]['endTime']/60:.1f} minutes)")
    
    # Create JSON transcript
    json_segments = [{"startTime": s["startTime"], "endTime": s["endTime"], "body": s["body"], "speaker": s["speaker"]} for s in segments]
    transcript = {"segments": json_segments}
    
    with open("test_skip_silence_extended_random.json", "w") as f:
        json.dump(transcript, f, indent=2)
    print("JSON transcript created!")
    
    # Create audio in chunks
    chunks = create_audio_chunks(segments, 30)  # 30 segments per chunk
    print(f"Creating {len(chunks)} audio chunks...")
    
    chunk_files = []
    for i, chunk in enumerate(chunks):
        print(f"Processing chunk {i+1}/{len(chunks)}...")
        chunk_file = create_chunk_audio(chunk, i)
        if chunk_file:
            chunk_files.append(chunk_file)
        else:
            print(f"Failed to create chunk {i}")
            break
    
    if len(chunk_files) == len(chunks):
        print("Concatenating chunks...")
        if concatenate_chunks(chunk_files):
            print("Extended random audio file created successfully!")
            
            # Clean up chunk files
            import os
            for chunk_file in chunk_files:
                os.remove(chunk_file)
            os.remove("file_list.txt")
            print("Cleanup completed!")
        else:
            print("Failed to concatenate chunks")
    else:
        print("Failed to create all chunks")
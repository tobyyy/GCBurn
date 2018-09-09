﻿using System;
using System.CodeDom.Compiler;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using Benchmarking.Common;

namespace GCBurn.BurnTest 
{
    public class BurnTester
    {
        public const int AllocationSequenceLength = 1 << 20; // 2^20, i.e. ~ 1M items; must be a power of 2!
        public const double MinSize = 1;
        public const double MaxSize = 1 << 17; // 128KB
        public const double TimeSamplerFrequency = 1_000_000;
        public const double TimeSamplerUnitInSeconds = 1 / TimeSamplerFrequency;
        public const double MaxTime = 1000 * TimeSamplerFrequency; // 1000 seconds
        
        public static TimeSpan DefaultDuration = TimeSpan.FromSeconds(10);
        public TimeSpan Duration = DefaultDuration;
        public int ThreadCount = Environment.ProcessorCount;
        public StdRandom Random = new StdRandom(123); 
        public Func<StdRandom, IDistribution> CreateSizeSampler = Samplers.CreateStandardSizeSampler; 
        public Func<StdRandom, IDistribution> CreateTimeSampler = Samplers.CreateStandardTimeSampler;
        public long StaticSetSize = 0;
        public IndentedTextWriter Writer = new IndentedTextWriter(Console.Out, "  ");
        
        private bool _isInitialized;
        private object[] _staticSet;
        private (int ArraySize, sbyte BucketIndex, sbyte GenerationIndex)[] _allocations;
        private int[] _startIndexes;
        
        public static BurnTester New(long staticSetSize) => new BurnTester() {
            StaticSetSize = staticSetSize
        };
        
        public static BurnTester NewWarmup(long staticSetSize) => new BurnTester() {
            StaticSetSize = staticSetSize,
            Duration = TimeSpan.FromSeconds(1), 
            Writer = new IndentedTextWriter(TextWriter.Null),
        };

        public void TryInitialize()
        {
            if (_isInitialized)
                return;
            
            GC.Collect(); // To make it uniform w/ Go test

            _allocations = new (int, sbyte, sbyte) [AllocationSequenceLength];
            var sizeSampler = CreateSizeSampler(Random).Truncate(MinSize, MaxSize);
            var timeSampler = CreateTimeSampler(Random).Truncate(0, MaxTime);
            var releaseCycleTimeInSeconds = 1.0 * GarbageHolder.TicksPerReleaseCycle / Stopwatch.Frequency;
            var timeFactor = TimeSamplerUnitInSeconds / releaseCycleTimeInSeconds;
            for (var i = 0; i < AllocationSequenceLength; i++) {
                var size = (int) sizeSampler.Sample();
                var arraySize = GarbageAllocator.ByteSizeToArraySize(size);
                
                var time = timeSampler.Sample();
                var timeStr = ((long) (time * timeFactor)).ToString();
                var bucketIndex = (sbyte) (timeStr.Length - 1);
                var generationIndex = (sbyte) (timeStr[0] - '0');
                
                _allocations[i] = (arraySize, bucketIndex, generationIndex);
            }

            _startIndexes = Enumerable.Range(0, ThreadCount)
                .Select(_ => Random.Next() % AllocationSequenceLength)
                .ToArray();
            
            // Creating _staticSet
            var staticSet = new List<object>();
            var index = Random.Next() % AllocationSequenceLength;
            for (var currentSize = 0L; currentSize < StaticSetSize;) {
                var (arraySize, _, _) = _allocations[index];
                staticSet.Add(GarbageAllocator.CreateGarbage(arraySize));
                currentSize += GarbageAllocator.ArraySizeToByteSize(arraySize);
                index = (index + 1) % AllocationSequenceLength;
            }
            _staticSet = staticSet.ToArray();
            
            _isInitialized = true;
        }

        public void Run()
        {
            TryInitialize();
            GC.Collect();
            var duration = Duration.TotalSeconds;
            
            using (Writer.Section($"Test settings:")) {
                Writer.AppendMetric("Duration", duration, "s");
                Writer.AppendMetric("Thread count", ThreadCount, "");
                using (Writer.Section($"Static set:")) {
                    Writer.AppendMetric("Total size", StaticSetSize / Sizes.GB, "GB");
                    Writer.AppendMetric("Object count", _staticSet.Length / Sizes.Mega, "M");
                }
            }

            var allocators = Enumerable.Range(0, ThreadCount)
                .Select(i => new GarbageAllocator(_allocations, _startIndexes[i]))
                .ToArray();
            var threads = allocators
                .Select(a => new Thread(() => a.Run(Duration)))
                .ToArray();

            var gcCounts = GCInfo.GetGCCounts();
            var ramUsedBefore = GC.GetTotalMemory(false);
            var startTimestamp = Stopwatch.GetTimestamp();
            foreach (var thread in threads)
                thread.Start();
            foreach (var thread in threads)
                thread.Join();
            gcCounts = GCInfo.GetGCCountsSince(gcCounts);
            var ramUsedAfter = GC.GetTotalMemory(false);

            Writer.AppendLine();

            // Normalizing GCPauses (converting time to ms, removing offset)
            var ticksToMilliseconds = 1000.0 / Stopwatch.Frequency;
            foreach (var a in allocators) {
                a.GCPauses = a.GCPauses
                    .Select(p => p
                        .MoveBy(-startTimestamp)
                        .ExpandBy(-0.5)
                        .ScaleBy(ticksToMilliseconds))
                    .ToCanonicalSorted()
                    .ToList();
            }

            // Computing thread & global pauses
            var allPauses = allocators.SelectMany(a => a.GCPauses);
            var threadPauses = allocators
                .Select(a => a.GCPauses
                    .Select(p => p.Size)
                    .OrderBy(d => d)
                    .ToArray())
                .ToArray();
            var pauseCountPerSecondPerThread = allocators
                .SelectMany(a => a.GCPauses
                    .GroupBy(p => Math.Floor(p.Start / 1000))
                    .Select(g => (double) g.Count())
                ).ToArray();
            var globalPauses = allocators
                .Select(a => a.GCPauses)
                .IntersectSortedTuples()
                .Select(p => p.Size)
                .OrderBy(d => d)
                .ToArray();
            var allocationSizes = _allocations
                .Select(a => (double) GarbageAllocator.ArraySizeToByteSize(a.ArraySize))
                .OrderBy(s => s)
                .ToArray();
            var allocationHoldDurations = _allocations
                .Select(a => Math.Pow(10, a.BucketIndex) * a.GenerationIndex 
                    * GarbageHolder.TicksPerReleaseCycle * ticksToMilliseconds)
                .OrderBy(s => s)
                .ToArray();

            using (Writer.Section($"Allocation speed:")) {
                Writer.AppendMetric("Operations per second", allocators.Sum(a => a.AllocationCount) / duration / Sizes.Mega, "M/s");
                Writer.AppendMetric("Bytes per second", allocators.Sum(a => a.ByteCount)  / duration / Sizes.GB, "GB/s");
                using (Writer.Section($"Allocation stats:")) {
                    Writer.AppendHistogram("Size:", allocationSizes, "B");
                    Writer.AppendHistogram("Hold duration:", allocationHoldDurations, "ms");
                }
            }
            using (Writer.Section($"GC stats:")) {
                Writer.AppendValue("RAM used", $"{(ramUsedBefore / Sizes.GB):0.###} -> {(ramUsedAfter / Sizes.GB):0.###} GB");
                using (Writer.Section($"GC rate:")) {
                    foreach (var (c, i) in gcCounts.WithIndexes())
                        Writer.AppendMetric($"Gen{i}, # per second", c / duration, "/s");
                }
                using (Writer.Section($"Thread pauses:")) {
                    Writer.AppendMetric("% of time frozen", threadPauses.SelectMany(p => p).Sum() / 1000 / ThreadCount / duration * 100, "%");
                    Writer.AppendHistogram("# per second:", pauseCountPerSecondPerThread, "/s");
                }
                using (Writer.Section($"Global pauses:")) {
                    Writer.AppendMetric("% of time frozen", globalPauses.Sum() / 1000 / duration * 100, "%");
                    Writer.AppendMetric("# per second", globalPauses.Length / duration, "/s");
                    Writer.AppendHistogram("Pause duration:", globalPauses, "ms");
                }
            }
            Writer.AppendLine();
        }
    }
}